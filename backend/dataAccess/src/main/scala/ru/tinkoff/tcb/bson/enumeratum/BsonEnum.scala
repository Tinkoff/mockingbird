package ru.tinkoff.tcb.bson.enumeratum

import enumeratum.*

import ru.tinkoff.tcb.bson.*

trait BsonEnum[A <: EnumEntry] { self: Enum[A] =>
  implicit val bsonEncoder: BsonEncoder[A] =
    EnumHandler.writer(this)

  implicit val bsonDecoder: BsonDecoder[A] =
    EnumHandler.reader(this)
}
