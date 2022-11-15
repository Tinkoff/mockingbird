package ru.tinkoff.tcb.mockingbird.api.request

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import eu.timepit.refined.*
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.*
import eu.timepit.refined.numeric.NonNegative
import io.circe.Json
import io.circe.refined.*
import sttp.tapir.Schema.annotations.description
import sttp.tapir.codec.refined.*
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.bson.annotation.BsonKey
import ru.tinkoff.tcb.bson.derivation.bsonEncoder
import ru.tinkoff.tcb.circe.bson.*
import ru.tinkoff.tcb.generic.PropSubset
import ru.tinkoff.tcb.mockingbird.model.Callback
import ru.tinkoff.tcb.mockingbird.model.DestinationConfiguration
import ru.tinkoff.tcb.mockingbird.model.Scenario
import ru.tinkoff.tcb.mockingbird.model.ScenarioInput
import ru.tinkoff.tcb.mockingbird.model.ScenarioOutput
import ru.tinkoff.tcb.mockingbird.model.Scope
import ru.tinkoff.tcb.mockingbird.model.SourceConfiguration
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.protocol.bson.*
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.utils.id.SID

@derive(decoder, encoder, schema)
case class UpdateScenarioRequest(
    @description("Тип конфигурации")
    scope: Scope,
    @description("Количество возможных срабатываний. Имеет смысл только для scope=countdown")
    times: Option[Int Refined NonNegative] = Some(refineMV(1)),
    service: String,
    @description("Имя сценария, отображается в логах, полезно для отладки")
    name: String Refined NonEmpty,
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
    labels: Seq[String]
)
object UpdateScenarioRequest {
  implicitly[PropSubset[UpdateScenarioRequest, ScenarioPatch]]
}

@derive(bsonEncoder)
case class ScenarioPatch(
    @BsonKey("_id") id: SID[Scenario],
    scope: Scope,
    times: Option[Int] = Some(1),
    service: String,
    name: String,
    source: SID[SourceConfiguration],
    seed: Option[Json],
    input: ScenarioInput,
    state: Option[Map[JsonOptic, Map[Keyword.Json, Json]]],
    persist: Option[Map[JsonOptic, Json]],
    destination: Option[SID[DestinationConfiguration]],
    output: Option[ScenarioOutput],
    callback: Option[Callback],
    labels: Seq[String]
)
