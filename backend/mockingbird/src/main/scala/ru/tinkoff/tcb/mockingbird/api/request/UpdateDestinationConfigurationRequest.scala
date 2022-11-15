package ru.tinkoff.tcb.mockingbird.api.request

import cats.data.NonEmptyVector
import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import sttp.tapir.Schema.annotations.description
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.generic.PropSubset
import ru.tinkoff.tcb.mockingbird.model.DestinationConfiguration
import ru.tinkoff.tcb.mockingbird.model.EventDestinationRequest
import ru.tinkoff.tcb.mockingbird.model.ResourceRequest
import ru.tinkoff.tcb.protocol.schema.*

@derive(decoder, encoder, schema)
case class UpdateDestinationConfigurationRequest(
    @description("Описание конфигурации")
    description: String,
    service: String,
    @description("Спецификация запроса")
    request: EventDestinationRequest,
    @description("Спецификация инициализатора")
    init: Option[NonEmptyVector[ResourceRequest]],
    @description("Спецификация деинициализатора")
    shutdown: Option[NonEmptyVector[ResourceRequest]],
)

object UpdateDestinationConfigurationRequest {
  implicitly[PropSubset[UpdateDestinationConfigurationRequest, DestinationConfiguration]]
}
