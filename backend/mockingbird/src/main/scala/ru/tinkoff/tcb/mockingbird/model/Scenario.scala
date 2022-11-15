package ru.tinkoff.tcb.mockingbird.model

import java.time.Instant

import com.github.dwickern.macros.NameOf.*
import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import io.circe.Json
import mouse.boolean.*
import sttp.tapir.Schema.annotations.description
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.bson.annotation.BsonKey
import ru.tinkoff.tcb.bson.derivation.bsonDecoder
import ru.tinkoff.tcb.bson.derivation.bsonEncoder
import ru.tinkoff.tcb.circe.bson.*
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.protocol.bson.*
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.utils.id.SID
import ru.tinkoff.tcb.utils.unpack.*
import ru.tinkoff.tcb.validation.Rule

@derive(bsonDecoder, bsonEncoder, encoder, decoder, schema)
case class Scenario(
    @BsonKey("_id")
    @description("id мока")
    id: SID[Scenario],
    @description("Время создания мока")
    created: Instant,
    @description("Тип конфигурации")
    scope: Scope,
    @description("Количество возможных срабатываний. Имеет смысл только для scope=countdown")
    times: Option[Int] = Some(1),
    service: String,
    @description("Имя сценария, отображается в логах, полезно для отладки")
    name: String,
    @description("Имя источника событий")
    source: SID[SourceConfiguration],
    seed: Option[Json],
    @description("Спецификация события")
    input: ScenarioInput,
    @description("Предикат для поиска состояния")
    state: Option[Map[JsonOptic, Map[Keyword.Json, Json]]],
    @description("Данные, записываемые в базу")
    persist: Option[Map[JsonOptic, Json]],
    @description("Имя назначения ответа")
    destination: Option[SID[DestinationConfiguration]],
    @description("Спецификация ответа")
    output: Option[ScenarioOutput],
    @description("Спецификация колбека")
    callback: Option[Callback],
    @description("Тэги")
    labels: Seq[String] = Seq.empty
)

object Scenario extends CallbackChecker {
  private val destOutp: Rule[Scenario] = (s: Scenario) =>
    (s.destination, s.output) match {
      case Some(_) <*> Some(_) | None <*> None => Vector.empty
      case None <*> Some(_) =>
        Vector(
          s"Поле ${nameOf[Scenario](_.destination)} должно быть заполнено при наличии ${nameOf[Scenario](_.output)}"
        )
      case Some(_) <*> None =>
        Vector(
          s"Поле ${nameOf[Scenario](_.destination)} должно быть заполнено ТОЛЬКО при наличии ${nameOf[Scenario](_.output)}"
        )
    }

  private def checkSourceId(sources: Set[SID[SourceConfiguration]]): Rule[Scenario] =
    (s: Scenario) => sources(s.source) !? Vector(s"${s.source} не настроен")

  private def checkDestinationId(destinations: Set[SID[DestinationConfiguration]]): Rule[Scenario] =
    (s: Scenario) =>
      s.destination.map(destinations).getOrElse(true) !? Vector(s"${s.destination.getOrElse("")} не настроен")

  private val timesGreaterZero: Rule[Scenario] =
    _.times.exists(_ <= 0).valueOrZero(Vector("times должно быть больше 0"))

  private val stateNonEmpty: Rule[Scenario] =
    _.state.exists(_.isEmpty).valueOrZero(Vector("Предикат state не может быть пустым"))

  private val persistNonEmpty: Rule[Scenario] =
    _.persist.exists(_.isEmpty).valueOrZero(Vector("Спецификация persist не может быть пустой"))

  def validationRules(
      sources: Set[SID[SourceConfiguration]],
      destinations: Set[SID[DestinationConfiguration]]
  ): Rule[Scenario] =
    Vector(
      destOutp,
      (s: Scenario) => checkCallback(s.callback, destinations),
      checkSourceId(sources),
      checkDestinationId(destinations),
      timesGreaterZero,
      stateNonEmpty,
      persistNonEmpty
    ).reduce(_ |+| _)
}
