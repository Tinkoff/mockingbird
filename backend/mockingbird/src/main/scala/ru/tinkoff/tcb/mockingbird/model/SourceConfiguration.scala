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
final case class SourceConfiguration(
    @BsonKey("_id") name: SID[SourceConfiguration],
    created: Instant,
    description: String,
    service: String,
    request: EventSourceRequest,
    init: Option[NonEmptyVector[ResourceRequest]],
    shutdown: Option[NonEmptyVector[ResourceRequest]],
    reInitTriggers: Option[NonEmptyVector[ResponseSpec]]
)

object SourceConfiguration {
  implicit def sourceConfigurationBsonEncoder(implicit aes: AES): BsonEncoder[SourceConfiguration] =
    DerivedEncoder.genBsonEncoder[SourceConfiguration]

  implicit def sourceConfigurationBsonDecoder(implicit aes: AES): BsonDecoder[SourceConfiguration] =
    DerivedDecoder.genBsonDecoder[SourceConfiguration]
}
