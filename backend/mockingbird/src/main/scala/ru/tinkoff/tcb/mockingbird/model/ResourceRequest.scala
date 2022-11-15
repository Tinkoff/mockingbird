package ru.tinkoff.tcb.mockingbird.model

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.bson.BsonDecoder
import ru.tinkoff.tcb.bson.BsonEncoder
import ru.tinkoff.tcb.bson.derivation.DerivedDecoder
import ru.tinkoff.tcb.bson.derivation.DerivedEncoder
import ru.tinkoff.tcb.utils.crypto.AES

@derive(decoder, encoder, schema)
final case class ResourceRequest(
    url: SecureString,
    method: HttpMethod,
    headers: Map[String, SecureString] = Map(),
    body: Option[SecureString] = None,
)

object ResourceRequest {
  implicit def resourceRequestBsonEncoder(implicit aes: AES): BsonEncoder[ResourceRequest] =
    DerivedEncoder.genBsonEncoder[ResourceRequest]

  implicit def resourceRequestBsonDecoder(implicit aes: AES): BsonDecoder[ResourceRequest] =
    DerivedDecoder.genBsonDecoder[ResourceRequest]
}
