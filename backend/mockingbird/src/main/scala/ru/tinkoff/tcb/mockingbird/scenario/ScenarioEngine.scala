package ru.tinkoff.tcb.mockingbird.scenario

import java.nio.charset.Charset
import java.util.Base64

import io.circe.Json
import io.circe.syntax.*
import kantan.xpath.Node as KNode
import kantan.xpath.XmlSource
import mouse.boolean.*
import mouse.option.*
import sttp.client3.*
import sttp.client3.circe.*
import sttp.model.Method
import zio.interop.catz.core.*

import ru.tinkoff.tcb.criteria.*
import ru.tinkoff.tcb.criteria.Typed.*
import ru.tinkoff.tcb.logging.MDCLogging
import ru.tinkoff.tcb.mockingbird.api.Tracing
import ru.tinkoff.tcb.mockingbird.api.WLD
import ru.tinkoff.tcb.mockingbird.dal.PersistentStateDAO
import ru.tinkoff.tcb.mockingbird.dal.ScenarioDAO
import ru.tinkoff.tcb.mockingbird.error.CallbackError
import ru.tinkoff.tcb.mockingbird.error.ScenarioExecError
import ru.tinkoff.tcb.mockingbird.error.ScenarioSearchError
import ru.tinkoff.tcb.mockingbird.misc.Renderable.ops.*
import ru.tinkoff.tcb.mockingbird.model.Callback
import ru.tinkoff.tcb.mockingbird.model.CallbackResponseMode
import ru.tinkoff.tcb.mockingbird.model.DestinationConfiguration
import ru.tinkoff.tcb.mockingbird.model.HttpCallback
import ru.tinkoff.tcb.mockingbird.model.JsonCallbackRequest
import ru.tinkoff.tcb.mockingbird.model.JsonOutput
import ru.tinkoff.tcb.mockingbird.model.MessageCallback
import ru.tinkoff.tcb.mockingbird.model.PersistentState
import ru.tinkoff.tcb.mockingbird.model.RawOutput
import ru.tinkoff.tcb.mockingbird.model.Scenario
import ru.tinkoff.tcb.mockingbird.model.ScenarioOutput
import ru.tinkoff.tcb.mockingbird.model.Scope
import ru.tinkoff.tcb.mockingbird.model.SourceConfiguration
import ru.tinkoff.tcb.mockingbird.model.XMLCallbackRequest
import ru.tinkoff.tcb.mockingbird.model.XmlOutput
import ru.tinkoff.tcb.mockingbird.stream.SDFetcher
import ru.tinkoff.tcb.utils.id.SID
import ru.tinkoff.tcb.utils.transformation.json.*
import ru.tinkoff.tcb.utils.transformation.string.*
import ru.tinkoff.tcb.utils.transformation.xml.*
import ru.tinkoff.tcb.utils.xml.emptyKNode
import ru.tinkoff.tcb.utils.xttp.*

trait CallbackEngine {
  def recurseCallback(
      state: PersistentState,
      callback: Callback,
      data: Json,
      xdata: KNode
  ): RIO[WLD, Unit]
}

final class ScenarioEngine(
    scenarioDAO: ScenarioDAO[Task],
    stateDAO: PersistentStateDAO[Task],
    resolver: ScenarioResolver,
    fetcher: SDFetcher,
    private val httpBackend: SttpBackend[Task, ?]
) extends CallbackEngine {
  private val log = MDCLogging.`for`[WLD](this)

  def perform(source: SID[SourceConfiguration], message: String): RIO[WLD, Unit] = {
    val f = resolver.findScenarioAndState(source, message) _

    for {
      _ <- Tracing.update(_.addToPayload(("source" -> source)))
      _ <- log.info("Получено сообщение из {}", source)
      (scenario, stateOp) <- f(Scope.Countdown)
        .filterOrElse(_.isDefined)(f(Scope.Ephemeral).filterOrElse(_.isDefined)(f(Scope.Persistent)))
        .someOrFail(ScenarioSearchError(s"Не удалось подобрать сценарий для сообщения из $source"))
      _ <- log.info("Выполнение сценария '{}'", scenario.name)
      seed     = scenario.seed.map(_.eval)
      bodyJson = scenario.input.extractJson(message)
      bodyXml  = scenario.input.extractXML(message)
      state <- ZIO.fromOption(stateOp).orElse(PersistentState.fresh)
      data  = Json.obj("message" := bodyJson, "state" := state.data, "seed" := seed)
      xdata = bodyXml.getOrElse(emptyKNode)
      _ <-
        scenario.persist
          .cata(
            spec => stateDAO.upsertBySpec(state.id, spec.fill(data).fill(xdata)).map(_.successful),
            ZIO.succeed(true)
          )
      _ <- scenario.persist
        .map(_.keys.map(_.path).filter(_.startsWith("_")).toVector)
        .filter(_.nonEmpty)
        .fold(ZIO.attempt(()))(_.traverse_(stateDAO.createIndexForDataField))
      dests <- fetcher.getDestinations
      _ <- ZIO.when(scenario.destination.isDefined && !dests.exists(_.name == scenario.destination.get))(
        ZIO.fail(ScenarioExecError(s"Не сконфигурирован destination с именем ${scenario.destination.get}"))
      )
      destOut = scenario.destination.flatMap(dn => dests.find(_.name == dn)) zip scenario.output
      _ <- ZIO.when(destOut.isDefined) {
        val (dest, out) = destOut.get
        sendTo(dest, out, data, xdata)
      }
      _ <- ZIO.when(scenario.scope == Scope.Countdown)(
        scenarioDAO.updateById(scenario.id, prop[Scenario](_.times).inc(-1))
      )
      _ <- ZIO.when(scenario.callback.isDefined)(recurseCallback(state, scenario.callback.get, data, xdata))
    } yield ()
  }

  def recurseCallback(
      state: PersistentState,
      callback: Callback,
      data: Json,
      xdata: KNode
  ): RIO[WLD, Unit] =
    callback match {
      case MessageCallback(destinationId, output, callback, delay) =>
        for {
          _     <- ZIO.when(delay.isDefined)(ZIO.sleep(Duration.fromScala(delay.get)))
          _     <- log.info("Выполняется MessageCallback с destinationId={}", destinationId)
          dests <- fetcher.getDestinations
          _ <- ZIO.when(!dests.exists(_.name == destinationId))(
            ZIO.fail(CallbackError(s"Не сконфигурирован destination с именем ${destinationId}"))
          )
          destination = dests.find(_.name == destinationId).get
          _ <- sendTo(destination, output, data, xdata)
          _ <- ZIO.when(callback.isDefined)(recurseCallback(state, callback.get, data, xdata))
        } yield ()
      case HttpCallback(request, responseMode, persist, callback, delay) =>
        for {
          _ <- ZIO.when(delay.isDefined)(ZIO.sleep(Duration.fromScala(delay.get)))
          requestUrl = request.url.value.substitute(data, xdata)
          _ <- log.info("Выполняется HttpCallback на {}", requestUrl)
          res <-
            basicRequest
              .headers(request.headers)
              .method(Method(request.method.entryName), uri"$requestUrl")
              .pipe(r =>
                request match {
                  case JsonCallbackRequest(_, _, _, body) => r.body(body.substitute(data).substitute(xdata).noSpaces)
                  case XMLCallbackRequest(_, _, _, body) =>
                    r.body(body.toNode.substitute(data).substitute(xdata).mkString)
                  case _ => r
                }
              )
              .response(asString)
              .send(httpBackend)
              .filterOrElseWith(_.isSuccess)(r => ZIO.fail(CallbackError(s"$requestUrl ответил ошибкой: $r")))
          bodyStr = res.body.getOrElse(throw new UnsupportedOperationException("Не может быть"))
          jsonBody =
            responseMode
              .contains(CallbackResponseMode.Json)
              .option(io.circe.parser.parse(bodyStr).toOption)
              .flatten
          xmlBody =
            responseMode
              .contains(CallbackResponseMode.Xml)
              .option(XmlSource[String].asNode(bodyStr).toOption)
              .flatten
          data1  = jsonBody.cata(j => data.mapObject(("req" -> j) +: _), data)
          xdata1 = xmlBody.getOrElse(xdata)
          _ <- ZIO.when(persist.isDefined) {
            stateDAO.upsertBySpec(state.id, persist.get.fill(data1).fill(xdata1))
          }
          _ <- ZIO.when(callback.isDefined)(recurseCallback(state, callback.get, data1, xdata1))
        } yield ()
    }

  private def sendTo(dest: DestinationConfiguration, out: ScenarioOutput, data: Json, xdata: KNode): RIO[WLD, Unit] =
    ZIO.when(out.delay.isDefined)(ZIO.sleep(Duration.fromScala(out.delay.get))) *> basicRequest
      .pipe(rt =>
        dest.request.body.fold {
          rt.body(
            out match {
              case RawOutput(payload, _)  => payload
              case JsonOutput(payload, _) => payload.substitute(data).substitute(xdata).noSpaces
              case XmlOutput(payload, _)  => payload.toNode.substitute(data).substitute(xdata).mkString
            }
          )
        } { drb =>
          val bodyJson = out match {
            case RawOutput(payload, _)  => Json.fromString(payload)
            case JsonOutput(payload, _) => payload.substitute(data).substitute(xdata)
            case XmlOutput(payload, _)  => Json.fromString(payload.toNode.substitute(data).substitute(xdata).mkString)
          }

          rt.body(
            drb.substitute(
              if (dest.request.stringifybody.contains(true)) Json.obj("_message" := bodyJson.noSpaces)
              else if (dest.request.encodeBase64.contains(true))
                Json.obj(
                  "_message" := bodyJson.asString.map(b64Enc).getOrElse(b64Enc(bodyJson.noSpaces))
                )
              else Json.obj("_message" := bodyJson)
            )
          )
        }
      )
      .headersReplacing(dest.request.headers.view.mapValues(_.asString).toMap)
      .method(Method(dest.request.method.entryName), uri"${dest.request.url}")
      .response(asString)
      .send(httpBackend)
      .filterOrElseWith(_.isSuccess)(r =>
        ZIO.fail(ScenarioExecError(s"Destination ${dest.name} ответил ошибкой: $r"))
      ) *>
      log.info("Отправлен ответ в {}", dest.name)

  private def b64Enc(s: String): String =
    new String(Base64.getEncoder.encode(s.getBytes(Charset.defaultCharset())), Charset.defaultCharset())
}

object ScenarioEngine {
  val live = ZLayer {
    for {
      sd         <- ZIO.service[ScenarioDAO[Task]]
      psd        <- ZIO.service[PersistentStateDAO[Task]]
      resolver   <- ZIO.service[ScenarioResolver]
      fetcher    <- ZIO.service[SDFetcher]
      sttpClient <- ZIO.service[SttpBackend[Task, Any]]
    } yield new ScenarioEngine(sd, psd, resolver, fetcher, sttpClient)
  }
}
