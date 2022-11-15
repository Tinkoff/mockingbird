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
import ru.tinkoff.tcb.mockingbird.model.SourceConfiguration
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.id.SID

@derive(decoder, encoder, schema)
case class CreateSourceConfigurationRequest(
    @description("Уникальное название конфигурации")
    name: SID[SourceConfiguration],
    @description("Описание конфигурации")
    description: String,
    service: String,
    @description("Спецификация запроса")
    request: EventSourceRequest,
    @description("Спецификация инициализатора")
    init: Option[NonEmptyVector[ResourceRequest]],
    @description("Спецификация деинициализатора")
    shutdown: Option[NonEmptyVector[ResourceRequest]]
)

object CreateSourceConfigurationRequest {
  implicitly[PropSubset[CreateSourceConfigurationRequest, SourceConfiguration]]
}
