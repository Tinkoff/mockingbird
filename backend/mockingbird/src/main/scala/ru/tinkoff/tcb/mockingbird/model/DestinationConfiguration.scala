package ru.tinkoff.tcb.mockingbird.model

import java.time.Instant

import cats.data.NonEmptyVector
import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.bson.BsonDecoder
import ru.tinkoff.tcb.bson.BsonEncoder
import ru.tinkoff.tcb.bson.annotation.BsonKey
import ru.tinkoff.tcb.bson.derivation.DerivedDecoder
import ru.tinkoff.tcb.bson.derivation.DerivedEncoder
import ru.tinkoff.tcb.protocol.bson.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.crypto.AES
import ru.tinkoff.tcb.utils.id.SID

@derive(encoder, decoder, schema)
case class DestinationConfiguration(
    @BsonKey("_id") name: SID[DestinationConfiguration],
    created: Instant,
    description: String,
    service: String,
    request: EventDestinationRequest,
    init: Option[NonEmptyVector[ResourceRequest]],
    shutdown: Option[NonEmptyVector[ResourceRequest]],
)

object DestinationConfiguration {
  implicit def destinationConfigurationBsonEncoder(implicit aes: AES): BsonEncoder[DestinationConfiguration] =
    DerivedEncoder.genBsonEncoder[DestinationConfiguration]

  implicit def destinationConfigurationBsonDecoder(implicit aes: AES): BsonDecoder[DestinationConfiguration] =
    DerivedDecoder.genBsonDecoder[DestinationConfiguration]
}
