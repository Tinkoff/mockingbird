package ru.tinkoff.tcb.mockingbird.api.request

import scala.util.matching.Regex

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
import ru.tinkoff.tcb.mockingbird.model.HttpMethod
import ru.tinkoff.tcb.mockingbird.model.HttpStub
import ru.tinkoff.tcb.mockingbird.model.HttpStubRequest
import ru.tinkoff.tcb.mockingbird.model.HttpStubResponse
import ru.tinkoff.tcb.mockingbird.model.Scope
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.protocol.bson.*
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.utils.id.SID

@derive(decoder, encoder, schema)
case class UpdateStubRequest(
    @description("Тип конфигурации")
    scope: Scope,
    @description("Количество возможных срабатываний. Имеет смысл только для scope=countdown")
    times: Option[Int Refined NonNegative] = Some(refineMV(1)),
    @description("Название мока")
    name: String Refined NonEmpty,
    @description("HTTP метод")
    method: HttpMethod,
    @description("Суффикс пути, по которому срабатывает мок")
    path: Option[String Refined NonEmpty],
    pathPattern: Option[Regex],
    seed: Option[Json],
    @description("Предикат для поиска состояния")
    state: Option[Map[JsonOptic, Map[Keyword.Json, Json]]],
    @description("Спецификация запроса")
    request: HttpStubRequest,
    @description("Данные, записываемые в базу")
    persist: Option[Map[JsonOptic, Json]],
    @description("Спецификация ответа")
    response: HttpStubResponse,
    @description("Спецификация колбека")
    callback: Option[Callback],
    @description("Тэги")
    labels: Seq[String]
)
object UpdateStubRequest {
  implicitly[PropSubset[UpdateStubRequest, StubPatch]]
}

@derive(bsonEncoder)
case class StubPatch(
    @BsonKey("_id") id: SID[HttpStub],
    scope: Scope,
    times: Option[Int],
    name: String,
    method: HttpMethod,
    path: Option[String],
    pathPattern: Option[Regex],
    seed: Option[Json],
    state: Option[Map[JsonOptic, Map[Keyword.Json, Json]]],
    request: HttpStubRequest,
    persist: Option[Map[JsonOptic, Json]],
    response: HttpStubResponse,
    callback: Option[Callback],
    labels: Seq[String]
)
