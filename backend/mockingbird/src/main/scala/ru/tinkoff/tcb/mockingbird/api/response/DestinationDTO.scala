package ru.tinkoff.tcb.mockingbird.api.response

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.bson.annotation.BsonKey
import ru.tinkoff.tcb.bson.derivation.bsonDecoder
import ru.tinkoff.tcb.mockingbird.model.DestinationConfiguration
import ru.tinkoff.tcb.utils.id.SID

@derive(encoder, decoder, schema, bsonDecoder)
case class DestinationDTO(@BsonKey("_id") name: SID[DestinationConfiguration], description: String)
