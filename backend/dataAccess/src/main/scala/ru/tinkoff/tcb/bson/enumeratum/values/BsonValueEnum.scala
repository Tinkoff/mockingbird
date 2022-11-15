package ru.tinkoff.tcb.bson.enumeratum.values

import scala.util.Try

import enumeratum.values.*

import ru.tinkoff.tcb.bson.*
sealed trait BsonValueEnum[ValueType, EntryType <: ValueEnumEntry[ValueType]] {
  enum: ValueEnum[ValueType, EntryType] =>

  implicit def bsonEncoder: BsonEncoder[EntryType]
  implicit def bsonDecoder: BsonDecoder[EntryType]
}

/**
 * Enum implementation for Int enum members that contains an implicit ReactiveMongo BSON Handler
 */
trait IntBsonValueEnum[EntryType <: IntEnumEntry] extends BsonValueEnum[Int, EntryType] {
  this: IntEnum[EntryType] =>

  implicit def bsonEncoder: BsonEncoder[EntryType] = EnumHandler.writer(this)
  implicit def bsonDecoder: BsonDecoder[EntryType] = EnumHandler.reader(this)
}

/**
 * Enum implementation for Long enum members that contains an implicit ReactiveMongo BSON Handler
 */
trait LongBsonValueEnum[EntryType <: LongEnumEntry] extends BsonValueEnum[Long, EntryType] {
  this: LongEnum[EntryType] =>

  implicit def bsonEncoder: BsonEncoder[EntryType] = EnumHandler.writer(this)
  implicit def bsonDecoder: BsonDecoder[EntryType] = EnumHandler.reader(this)
}

/*
  /**
 * Enum implementation for Short enum members that contains an implicit ReactiveMongo BSON Handler
 */
  trait ShortReactiveMongoBsonValueEnum[EntryType <: ShortEnumEntry]
      extends ReactiveMongoBsonValueEnum[Short, EntryType] { this: ShortEnum[EntryType] =>

        implicit def bsonEncoder: BsonEncoder[EntryType] = EnumHandler.writer(this)
        implicit def bsonDecoder: BsonDecoder[EntryType] = EnumHandler.reader(this)
  }
 */

/**
 * Enum implementation for String enum members that contains an implicit ReactiveMongo BSON Handler
 */
trait StringBsonValueEnum[EntryType <: StringEnumEntry] extends BsonValueEnum[String, EntryType] {
  this: StringEnum[EntryType] =>

  implicit def bsonEncoder: BsonEncoder[EntryType]       = EnumHandler.writer(this)
  implicit def bsonDecoder: BsonDecoder[EntryType]       = EnumHandler.reader(this)
  implicit def bsonKeyEncoder: BsonKeyEncoder[EntryType] = (t: EntryType) => t.value
  implicit def bsonKeyDecoder: BsonKeyDecoder[EntryType] = (value: String) => Try(withValue(value))
}

/*
  /**
 * Enum implementation for Char enum members that contains an implicit ReactiveMongo BSON Handler
 */
  trait CharReactiveMongoBsonValueEnum[EntryType <: CharEnumEntry]
      extends ReactiveMongoBsonValueEnum[Char, EntryType] { this: CharEnum[EntryType] =>

        implicit def bsonEncoder: BsonEncoder[EntryType] = EnumHandler.writer(this)
        implicit def bsonDecoder: BsonDecoder[EntryType] = EnumHandler.reader(this)
  }

  /**
 * Enum implementation for Byte enum members that contains an implicit ReactiveMongo BSON Handler
 */
  trait ByteReactiveMongoBsonValueEnum[EntryType <: ByteEnumEntry]
      extends ReactiveMongoBsonValueEnum[Byte, EntryType] { this: ByteEnum[EntryType] =>

        implicit def bsonEncoder: BsonEncoder[EntryType] = EnumHandler.writer(this)
        implicit def bsonDecoder: BsonDecoder[EntryType] = EnumHandler.reader(this)
  }
 */
