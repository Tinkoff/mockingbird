package ru.tinkoff.tcb.mockingbird.stream

import scala.util.control.NonFatal

import fs2.Stream
import io.circe.DecodingFailure
import io.circe.Error as CirceError
import io.circe.parser.parse
import mouse.all.optionSyntaxMouse
import mouse.boolean.*
import sttp.client3.*
import sttp.model.Method
import zio.interop.catz.*
import zio.interop.catz.implicits.*

import ru.tinkoff.tcb.logging.MDCLogging
import ru.tinkoff.tcb.mockingbird.api.Tracing
import ru.tinkoff.tcb.mockingbird.api.WLD
import ru.tinkoff.tcb.mockingbird.config.EventConfig
import ru.tinkoff.tcb.mockingbird.error.CallbackError
import ru.tinkoff.tcb.mockingbird.error.CompoundError
import ru.tinkoff.tcb.mockingbird.error.EventProcessingError
import ru.tinkoff.tcb.mockingbird.error.ScenarioExecError
import ru.tinkoff.tcb.mockingbird.error.ScenarioSearchError
import ru.tinkoff.tcb.mockingbird.error.SpawnError
import ru.tinkoff.tcb.mockingbird.model.EventSourceRequest
import ru.tinkoff.tcb.mockingbird.scenario.ScenarioEngine
import ru.tinkoff.tcb.utils.circe.JsonString
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic

final class EventSpawner(
    eventConfig: EventConfig,
    fetcher: SDFetcher,
    private val httpBackend: SttpBackend[Task, ?],
    engine: ScenarioEngine
) {
  private val log = MDCLogging.`for`[WLD](this)

  private def asStringBypass(bypassCodes: Set[Int]): ResponseAs[Either[String, String], Any] =
    asStringAlways("utf-8").mapWithMetadata { (s, m) =>
      if (m.isSuccess) Right(s) else if (bypassCodes(m.code.code)) Right("") else Left(s)
    }

  private val jvectorize: JsonOptic => String => Either[CirceError, Vector[String]] =
    (jEmumerator: JsonOptic) =>
      (s: String) =>
        for {
          parsed <- parse(s)
          _      <- jEmumerator.validate(parsed).either(DecodingFailure(s"Can't reach ${jEmumerator.path}", Nil), ())
          values = jEmumerator.getAll(parsed)
        } yield values.map(_.noSpaces)

  private val jextract: JsonOptic => String => Either[CirceError, String] =
    (jExtractor: JsonOptic) =>
      (s: String) =>
        for {
          parsed <- parse(s)
          value  <- jExtractor.getOpt(parsed).toRight(DecodingFailure(s"Can't extract ${jExtractor.path}", Nil))
        } yield value.noSpaces

  private val jdecode: String => Either[CirceError, String] = (s: String) =>
    for {
      parsed <- parse(s)
      decoded <- parsed match {
        case js @ JsonString(str) => parse(str).orElse(Right(js))
        case otherwise            => Right(otherwise)
      }
    } yield decoded.noSpaces

  private def fetch(req: EventSourceRequest): Task[Vector[String]] = {
    val request = basicRequest
      .headers(req.headers.view.mapValues(_.asString).toMap)
      .pipe(r => req.body.cata(b => r.body(b.asString), r))
      .method(Method(req.method.entryName), uri"${req.url.asString}")
      .response(asStringBypass(req.bypassCodes.getOrElse(Set())))

    for {
      response <- request.send(httpBackend)
      body <- ZIO
        .fromEither(response.body)
        .mapError(err => EventProcessingError(s"Запрос на ${req.url.asString} завершился ошибкой ($err)"))
      processed <- ZIO.fromEither {
        for {
          vectorized <- req.jenumerate.map(jvectorize).getOrElse((s: String) => Right(Vector(s)))(body)
          extracted  <- vectorized.traverse(req.jextract.map(jextract).getOrElse(Right(_: String)))
          decoded    <- req.jstringdecode.fold(extracted.traverse(jdecode), Right(extracted))
        } yield decoded
      }
    } yield processed
  }

  private def fetchStream: Stream[RIO[WLD, *], Unit] =
    Stream
      .awakeEvery[RIO[WLD, *]](eventConfig.fetchInterval)
      .evalMap(_ => fetcher.getSources)
      .evalMap(
        ZIO
          .validateParDiscard(_) { sourceConf =>
            (for {
              _   <- Tracing.init
              res <- fetch(sourceConf.request).mapError(SpawnError(sourceConf.name, _))
              neRes = res.filter(_.nonEmpty)
              _ <- ZIO.when(neRes.nonEmpty)(log.info(s"Отправлено в обработку: ${neRes.length}"))
              _ <- ZIO
                .validateDiscard(neRes) {
                  engine.perform(sourceConf.name, _)
                }
                .mapError(CompoundError(_))
            } yield ())
              .catchSomeDefect { case NonFatal(ex) =>
                ZIO.fail(SpawnError(sourceConf.name, ex))
              }
          }
          .mapError(CompoundError(_))
      )
      .handleErrorWith {
        case thr if recover.isDefinedAt(thr) =>
          Stream.eval(recover(thr)) ++ Stream.sleep[RIO[WLD, *]](eventConfig.fetchInterval) ++ fetchStream
        case CompoundError(errs) =>
          val recoverable = errs.filter(recover.isDefinedAt)
          val fatal       = errs.find(!recover.isDefinedAt(_))

          Stream.evalSeq(ZIO.foreach(recoverable)(recover)) ++ Stream.raiseError[RIO[WLD, *]](fatal.get).as(())
        case thr =>
          Stream.raiseError[RIO[WLD, *]](thr).as(())
      }

  def run: RIO[WLD, Unit] = fetchStream.compile.drain

  private lazy val recover: PartialFunction[Throwable, URIO[WLD, Unit]] = {
    case CompoundError(errs) if errs.forall(recover.isDefinedAt) =>
      ZIO.foreachDiscard(errs)(recover)
    case EventProcessingError(err) =>
      log.warn(s"Ошибка при обработке события: $err")
    case ScenarioExecError(err) =>
      log.warn(s"Ошибка при выполнении сценария: $err")
    case ScenarioSearchError(err) =>
      log.warn(s"Ошибка при поиске сценария: $err")
    case CallbackError(err) =>
      log.warn(s"Ошибка при выполнении колбэка: $err")
    case SpawnError(sid, err) =>
      log.errorCause(s"Ошибка при получении события из {}", err, sid)
    case NonFatal(t) =>
      log.errorCause("Ошибка при получении события", t)
  }
}

object EventSpawner {
  val live = ZLayer {
    for {
      config         <- ZIO.service[EventConfig]
      fetcher        <- ZIO.service[SDFetcher]
      sttpClient     <- ZIO.service[SttpBackend[Task, Any]]
      scenarioEngine <- ZIO.service[ScenarioEngine]
    } yield new EventSpawner(config, fetcher, sttpClient, scenarioEngine)
  }
}
