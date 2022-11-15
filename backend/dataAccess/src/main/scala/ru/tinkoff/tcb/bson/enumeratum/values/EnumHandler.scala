package ru.tinkoff.tcb.bson.enumeratum.values

import enumeratum.values.*
import org.mongodb.scala.bson.*

import ru.tinkoff.tcb.bson.*

object EnumHandler {

  /**
   * Returns a BSONReader for the provided ValueEnum based on the given base BSONReader for the Enum's value type
   */
  def reader[ValueType, EntryType <: ValueEnumEntry[ValueType]](
      `enum`: ValueEnum[ValueType, EntryType]
  )(implicit
      baseBsonReader: BsonDecoder[ValueType]
  ): BsonDecoder[EntryType] =
    (value: BsonValue) => baseBsonReader.fromBson(value).map(`enum`.withValue)

  /**
   * Returns a BSONWriter for the provided ValueEnum based on the given base BSONWriter for the Enum's value type
   */
  def writer[ValueType, EntryType <: ValueEnumEntry[ValueType]](
      `enum`: ValueEnum[ValueType, EntryType]
  )(implicit
      baseBsonWriter: BsonEncoder[ValueType]
  ): BsonEncoder[EntryType] =
    (value: EntryType) => baseBsonWriter.toBson(value.value)
}
