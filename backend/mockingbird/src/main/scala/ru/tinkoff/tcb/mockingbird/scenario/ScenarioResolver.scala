package ru.tinkoff.tcb.mockingbird.scenario

import io.circe.Json
import kantan.xpath.Node as KNode
import mouse.boolean.*
import mouse.option.*
import zio.interop.catz.core.*

import ru.tinkoff.tcb.criteria.*
import ru.tinkoff.tcb.criteria.Typed.*
import ru.tinkoff.tcb.logging.MDCLogging
import ru.tinkoff.tcb.mockingbird.api.WLD
import ru.tinkoff.tcb.mockingbird.dal.PersistentStateDAO
import ru.tinkoff.tcb.mockingbird.dal.ScenarioDAO
import ru.tinkoff.tcb.mockingbird.error.EarlyReturn
import ru.tinkoff.tcb.mockingbird.error.ScenarioSearchError
import ru.tinkoff.tcb.mockingbird.misc.Renderable.ops.*
import ru.tinkoff.tcb.mockingbird.model.PersistentState
import ru.tinkoff.tcb.mockingbird.model.Scenario
import ru.tinkoff.tcb.mockingbird.model.Scope
import ru.tinkoff.tcb.mockingbird.model.SourceConfiguration
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.utils.id.SID

class ScenarioResolver(scenarioDAO: ScenarioDAO[Task], stateDAO: PersistentStateDAO[Task]) {
  private val log = MDCLogging.`for`[WLD](this)

  private type StateSpec = Map[JsonOptic, Map[Keyword.Json, Json]]

  def findScenarioAndState(source: SID[SourceConfiguration], message: String)(
      scope: Scope
  ): RIO[WLD, Option[(Scenario, Option[PersistentState])]] =
    (for {
      _ <- log.info("Поиск сценариев для источника {} типа {}", source, scope)
      condition0 = prop[Scenario](_.source) === source && prop[Scenario](_.scope) === scope
      condition  = (scope == Scope.Countdown).fold(condition0 && prop[Scenario](_.times) > Option(0), condition0)
      scenarios0 <- scenarioDAO.findChunk(condition, 0, Int.MaxValue)
      _ <- ZIO.when(scenarios0.isEmpty)(
        log.info("Не найдены обработчики для источника {} типа {}", source, scope) *>
          ZIO.fail(EarlyReturn)
      )
      _ <- log.info("Кандидаты: {}", scenarios0.map(_.id))
      scenarios1 = scenarios0.filter(_.input.checkMessage(message))
      _ <- ZIO.when(scenarios1.isEmpty)(
        log.warn("После валидации сообщения не осталось кандидатов, проверьте сообщение: {}", message) *>
          ZIO.fail(EarlyReturn)
      )
      _ <- log.info("После валидации сообщения: {}", scenarios1.map(_.id))
      scenarios2 <- scenarios1.traverse { scenc =>
        val bodyJson = scenc.input.extractJson(message)
        val bodyXml  = scenc.input.extractXML(message)
        computeStateSpec(scenc.state, bodyJson, bodyXml)
          .cata(
            spec => findStates(scenc.id, spec).map(scenc -> _),
            ZIO.succeed(scenc -> Vector.empty[PersistentState])
          )
      }
      _ <- ZIO.when(scenarios2.exists(_._2.size > 1))(
        log.error("Для одного или нескольких сценариев найдено более одного подходящего состояния") *>
          ZIO.fail(
            ScenarioSearchError("Для одного или нескольких сценариев найдено более одного подходящего состояния")
          )
      )
      _ <- ZIO.when(scenarios2.count(_._2.nonEmpty) > 1)(
        log.error("Для более чем одного сценария нашлось подходящее состояние") *>
          ZIO.fail(ScenarioSearchError("Для более чем одного сценария нашлось подходящее состояние"))
      )
      _ <- ZIO.when(scenarios2.size > 1 && scenarios2.forall(c => c._1.state.isDefined && c._2.isEmpty))(
        log.error("Ни для одного сценария не найдено подходящего состояния") *>
          ZIO.fail(ScenarioSearchError("Ни для одного сценария не найдено подходящего состояния"))
      )
      _ <- ZIO.when(scenarios2.size > 1 && scenarios2.forall(_._1.state.isEmpty))(
        log.error("Найдено более одного не требующего состояния сценария") *>
          ZIO.fail(ScenarioSearchError("Найдено более одного не требующего состояния сценария"))
      )
      res = scenarios2.find(_._2.size == 1) orElse scenarios2.find(_._1.state.isEmpty)
    } yield res.map { case (scenario, states) => scenario -> states.headOption }).catchSome { case EarlyReturn =>
      ZIO.none
    }

  private def computeStateSpec(
      spec: Option[StateSpec],
      bodyJson: Option[Json],
      bodyXml: Option[KNode]
  ): Option[StateSpec] =
    (spec, bodyJson).mapN(_.fill(_)).orElse((spec, bodyXml).mapN(_.fill(_))).orElse(spec)

  private def findStates(id: SID[?], spec: StateSpec): RIO[WLD, Vector[PersistentState]] =
    for {
      _      <- log.info("Поиск state для {} по условию {}", id, spec.renderJson.noSpaces)
      states <- stateDAO.findBySpec(spec)
      _ <-
        if (states.nonEmpty) log.info("Найдены состояния для {}: {}", id, states.map(_.id))
        else log.info("Не найдено подходящих состояний для {}", id)
    } yield states
}

object ScenarioResolver {
  val live = ZLayer {
    for {
      sd  <- ZIO.service[ScenarioDAO[Task]]
      ssd <- ZIO.service[PersistentStateDAO[Task]]
    } yield new ScenarioResolver(sd, ssd)
  }
}
