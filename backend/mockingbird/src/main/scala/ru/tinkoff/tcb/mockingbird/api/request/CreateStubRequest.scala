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

import ru.tinkoff.tcb.generic.PropSubset
import ru.tinkoff.tcb.mockingbird.model.Callback
import ru.tinkoff.tcb.mockingbird.model.HttpMethod
import ru.tinkoff.tcb.mockingbird.model.HttpStub
import ru.tinkoff.tcb.mockingbird.model.HttpStubRequest
import ru.tinkoff.tcb.mockingbird.model.HttpStubResponse
import ru.tinkoff.tcb.mockingbird.model.Scope
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic

@derive(encoder, decoder, schema)
case class CreateStubRequest(
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
    labels: Seq[String] = Seq.empty
)
object CreateStubRequest {
  implicitly[PropSubset[CreateStubRequest, HttpStub]]
}
