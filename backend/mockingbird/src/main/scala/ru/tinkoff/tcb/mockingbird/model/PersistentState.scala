package ru.tinkoff.tcb.mockingbird.model

import java.time.Instant
import java.util.UUID

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import io.circe.Json
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.bson.annotation.BsonKey
import ru.tinkoff.tcb.bson.derivation.bsonDecoder
import ru.tinkoff.tcb.bson.derivation.bsonEncoder
import ru.tinkoff.tcb.circe.bson.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.id.SID

@derive(bsonDecoder, bsonEncoder, encoder, decoder, schema)
final case class PersistentState(
    @BsonKey("_id") id: SID[PersistentState],
    data: Json,
    created: Instant
)

object PersistentState {
  def fresh: Task[PersistentState] =
    ZIO.clockWith(_.instant).map(PersistentState(SID(UUID.randomUUID().toString), Json.obj(), _))
}
