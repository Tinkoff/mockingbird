package ru.tinkoff.tcb.mockingbird.api.response

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.bson.annotation.BsonKey
import ru.tinkoff.tcb.bson.derivation.bsonDecoder
import ru.tinkoff.tcb.mockingbird.model.SourceConfiguration
import ru.tinkoff.tcb.utils.id.SID

@derive(encoder, decoder, schema, bsonDecoder)
case class SourceDTO(@BsonKey("_id") name: SID[SourceConfiguration], description: String)
