package ru.tinkoff.tcb.mockingbird.model

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import io.circe.Json
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.bson.BsonDecoder
import ru.tinkoff.tcb.bson.BsonEncoder
import ru.tinkoff.tcb.bson.derivation.DerivedDecoder
import ru.tinkoff.tcb.bson.derivation.DerivedEncoder
import ru.tinkoff.tcb.circe.bson.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.crypto.AES

@derive(encoder, decoder, schema)
final case class EventDestinationRequest(
    url: SecureString,
    method: HttpMethod,
    headers: Map[String, SecureString],
    body: Option[Json],
    stringifybody: Option[Boolean],
    encodeBase64: Option[Boolean]
)

object EventDestinationRequest {
  implicit def eventDestinationRequestBsonEncoder(implicit aes: AES): BsonEncoder[EventDestinationRequest] =
    DerivedEncoder.genBsonEncoder[EventDestinationRequest]

  implicit def eventDestinationRequestBsonDecoder(implicit aes: AES): BsonDecoder[EventDestinationRequest] =
    DerivedDecoder.genBsonDecoder[EventDestinationRequest]
}
