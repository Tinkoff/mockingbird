package ru.tinkoff.tcb.mockingbird.api.request

import cats.data.NonEmptyVector
import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import sttp.tapir.Schema.annotations.description
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.generic.PropSubset
import ru.tinkoff.tcb.mockingbird.model.EventSourceRequest
import ru.tinkoff.tcb.mockingbird.model.ResourceRequest
import ru.tinkoff.tcb.mockingbird.model.ResponseSpec
import ru.tinkoff.tcb.mockingbird.model.SourceConfiguration
import ru.tinkoff.tcb.protocol.schema.*

@derive(decoder, encoder, schema)
case class UpdateSourceConfigurationRequest(
    @description("Описание конфигурации")
    description: String,
    service: String,
    @description("Спецификация запроса")
    request: EventSourceRequest,
    @description("Спецификация инициализатора")
    init: Option[NonEmptyVector[ResourceRequest]],
    @description("Спецификация деинициализатора")
    shutdown: Option[NonEmptyVector[ResourceRequest]],
    @description("Спецификации триггеров реинициализации")
    reInitTriggers: Option[NonEmptyVector[ResponseSpec]]
)

object UpdateSourceConfigurationRequest {
  implicitly[PropSubset[UpdateSourceConfigurationRequest, SourceConfiguration]]
}
