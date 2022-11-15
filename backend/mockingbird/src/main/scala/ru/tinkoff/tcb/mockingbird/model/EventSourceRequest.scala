package ru.tinkoff.tcb.mockingbird.model

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.bson.BsonDecoder
import ru.tinkoff.tcb.bson.BsonEncoder
import ru.tinkoff.tcb.bson.derivation.DerivedDecoder
import ru.tinkoff.tcb.bson.derivation.DerivedEncoder
import ru.tinkoff.tcb.protocol.bson.*
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.utils.crypto.AES

@derive(decoder, encoder, schema)
final case class EventSourceRequest(
    url: SecureString,
    method: HttpMethod,
    headers: Map[String, SecureString],
    body: Option[SecureString],
    jenumerate: Option[JsonOptic],
    jextract: Option[JsonOptic],
    bypassCodes: Option[Set[Int]],
    jstringdecode: Boolean = false
)

object EventSourceRequest {
  implicit def eventSourceRequestBsonEncoder(implicit aes: AES): BsonEncoder[EventSourceRequest] =
    DerivedEncoder.genBsonEncoder[EventSourceRequest]

  implicit def eventSourceRequestBsonDecoder(implicit aes: AES): BsonDecoder[EventSourceRequest] =
    DerivedDecoder.genBsonDecoder[EventSourceRequest]
}
