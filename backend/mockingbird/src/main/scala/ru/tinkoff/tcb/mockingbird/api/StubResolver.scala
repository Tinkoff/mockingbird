package ru.tinkoff.tcb.mockingbird.api

import com.github.dwickern.macros.NameOf.*
import io.circe.Json
import kantan.xpath.Node
import mouse.boolean.*
import mouse.option.*
import org.mongodb.scala.bson.*
import zio.interop.catz.core.*

import ru.tinkoff.tcb.criteria.*
import ru.tinkoff.tcb.criteria.Typed.*
import ru.tinkoff.tcb.logging.MDCLogging
import ru.tinkoff.tcb.mockingbird.dal.HttpStubDAO
import ru.tinkoff.tcb.mockingbird.dal.PersistentStateDAO
import ru.tinkoff.tcb.mockingbird.error.*
import ru.tinkoff.tcb.mockingbird.misc.Renderable.ops.*
import ru.tinkoff.tcb.mockingbird.model.HttpMethod
import ru.tinkoff.tcb.mockingbird.model.HttpStub
import ru.tinkoff.tcb.mockingbird.model.PersistentState
import ru.tinkoff.tcb.mockingbird.model.Scope
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.utils.id.SID

final class StubResolver(stubDAO: HttpStubDAO[Task], stateDAO: PersistentStateDAO[Task]) {
  private val log = MDCLogging.`for`[WLD](this)

  private type StateSpec = Map[JsonOptic, Map[Keyword.Json, Json]]

  def findStubAndState(
      method: HttpMethod,
      path: String,
      headers: Map[String, String],
      queryObject: Json,
      body: String
  )(
      scope: Scope
  ): RIO[WLD, Option[(HttpStub, Option[PersistentState])]] =
    (
      for {
        _ <- log.info("Поиск заглушек для запроса {} типа {}", path, scope)
        pathPatternExpr = Expression[HttpStub](
          None,
          "$expr" -> BsonDocument(
            "$regexMatch" -> BsonDocument(
              "input" -> path,
              "regex" -> s"$$${nameOf[HttpStub](_.pathPattern)}"
            )
          )
        )
        condition0 = prop[HttpStub](_.method) === method &&
          (prop[HttpStub](_.path) ==@ path || pathPatternExpr) &&
          prop[HttpStub](_.scope) === scope
        condition = (scope == Scope.Countdown).fold(condition0 && prop[HttpStub](_.times) > Option(0), condition0)
        candidates0 <- stubDAO.findChunk(condition, 0, Int.MaxValue)
        _ <- ZIO.when(candidates0.isEmpty)(
          log.info("Не найдены обработчики для запроса {} типа {}", path, scope) *> ZIO.fail(EarlyReturn)
        )
        _ <- log.info("Кандидаты: {}", candidates0.map(_.id))
        candidates1 = candidates0.filter(_.request.checkQueryParams(queryObject))
        _ <- ZIO.when(candidates1.isEmpty)(
          log.warn(
            "После проверки query параметров не осталось кандидатов, проверьте параметры: {}",
            queryObject.noSpaces
          ) *> ZIO.fail(EarlyReturn)
        )
        _ <- log.info("После проверки query параметров: {}", candidates1.map(_.id))
        candidates2 = candidates1.filter(_.request.checkHeaders(headers))
        _ <- ZIO.when(candidates2.isEmpty)(
          log.warn("После проверки заголовков не осталось кандидатов, проверьте заголовки: {}", headers) *>
            ZIO.fail(EarlyReturn)
        )
        _ <- log.info("После проверки заголовков: {}", candidates2.map(_.id))
        candidates3 = candidates2.filter(_.request.checkStringBody(body))
        _ <- ZIO.when(candidates3.isEmpty)(
          log.warn("После проверки тела запроса не осталось кандидатов, проверьте тело запроса: {}", body) *>
            ZIO.fail(EarlyReturn)
        )
        _ <- log.info("После валидации тела: {}", candidates3.map(_.id))
        candidates4 <- candidates3.traverse { stubc =>
          val bodyJson = stubc.request.extractJson(body)
          val bodyXml  = stubc.request.extractXML(body)
          computeStateSpec(stubc.state.map(_.fill(Json.obj("__query" -> queryObject))), bodyJson, bodyXml)
            .cata(
              spec => findStates(stubc.id, spec).map(stubc -> _),
              ZIO.succeed(stubc -> Vector.empty[PersistentState])
            )
        }
        _ <- ZIO.when(candidates4.exists(_._2.size > 1))(
          log.error("Для одной или нескольких заглушек найдено более одного подходящего состояния") *>
            ZIO.fail(StubSearchError("Для одной или нескольких заглушек найдено более одного подходящего состояния"))
        )
        _ <- ZIO.when(candidates4.count(_._2.nonEmpty) > 1)(
          log.error("Для более чем одной заглушки нашлось подходящее состояние") *>
            ZIO.fail(StubSearchError("Для более чем одной заглушки нашлось подходящее состояние"))
        )
        _ <- ZIO.when(candidates4.size > 1 && candidates4.forall(c => c._1.state.isDefined && c._2.isEmpty))(
          log.error("Ни для одной заглушки не найдено подходящего состояния") *>
            ZIO.fail(StubSearchError("Ни для одной заглушки не найдено подходящего состояния"))
        )
        _ <- ZIO.when(candidates4.size > 1 && candidates4.forall(_._1.state.isEmpty))(
          log.error("Найдено более одной не требующей состояния заглушки") *>
            ZIO.fail(StubSearchError("Найдено более одной не требующей состояния заглушки"))
        )
        res = candidates4.find(_._2.size == 1) orElse candidates4.find(_._1.state.isEmpty)
      } yield res.map { case (stub, states) => stub -> states.headOption }
    ).catchSome { case EarlyReturn =>
      ZIO.none
    }

  private def computeStateSpec(
      spec: Option[StateSpec],
      bodyJson: Option[Json],
      bodyXml: Option[Node]
  ): Option[StateSpec] =
    (spec, bodyJson)
      .mapN(_.fill(_))
      .orElse((spec, bodyXml).mapN(_.fill(_)))
      .orElse(spec)

  private def findStates(id: SID[?], spec: StateSpec): RIO[WLD, Vector[PersistentState]] =
    for {
      _      <- log.info("Поиск state для {} по условию {}", id, spec.renderJson.noSpaces)
      states <- stateDAO.findBySpec(spec)
      _ <-
        if (states.nonEmpty) log.info("Найдены состояния для {}: {}", id, states.map(_.id))
        else log.info("Не найдено подходящих состояний для {}", id)
    } yield states
}

object StubResolver {
  val live = ZLayer {
    for {
      hsd <- ZIO.service[HttpStubDAO[Task]]
      ssd <- ZIO.service[PersistentStateDAO[Task]]
    } yield new StubResolver(hsd, ssd)
  }
}
