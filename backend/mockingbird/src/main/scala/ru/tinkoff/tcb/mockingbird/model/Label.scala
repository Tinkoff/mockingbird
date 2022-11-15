package ru.tinkoff.tcb.mockingbird.model

import derevo.derive

import ru.tinkoff.tcb.bson.annotation.BsonKey
import ru.tinkoff.tcb.bson.derivation.bsonDecoder
import ru.tinkoff.tcb.bson.derivation.bsonEncoder
import ru.tinkoff.tcb.utils.id.SID

@derive(bsonDecoder, bsonEncoder)
case class Label(
    @BsonKey("_id") id: SID[Label],
    serviceSuffix: String,
    label: String
)
