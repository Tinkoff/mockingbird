package ru.tinkoff.tcb.mockingbird.model

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.bson.annotation.BsonKey
import ru.tinkoff.tcb.bson.derivation.bsonDecoder
import ru.tinkoff.tcb.bson.derivation.bsonEncoder

@derive(bsonDecoder, bsonEncoder, encoder, decoder, schema)
case class Service(@BsonKey("_id") suffix: String, name: String)
