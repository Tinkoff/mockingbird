package ru.tinkoff.tcb.bson.enumeratum

import scala.util.Failure
import scala.util.Try

import enumeratum.*
import org.mongodb.scala.bson.*

import ru.tinkoff.tcb.bson.*

/**
 * Holds BSON reader and writer for [[enumeratum.Enum]]
 */
@SuppressWarnings(Array("org.wartremover.warts.Throw"))
object EnumHandler {

  /**
   * Returns a BSONReader for a given enum [[Enum]]
   *
   * @param `enum`
   *   The enum
   * @param insensitive
   *   bind in a case-insensitive way, defaults to false
   */
  def reader[A <: EnumEntry](
      `enum`: Enum[A],
      insensitive: Boolean = false
  ): BsonDecoder[A] = {
    case BString(s) if insensitive => Try(`enum`.withNameInsensitive(s))
    case BString(s)                => Try(`enum`.withName(s))
    case _                         => Failure(new RuntimeException("String value expected"))
  }

  /**
   * Returns a BSONReader for a given enum [[Enum]] transformed to lower case
   *
   * @param `enum`
   *   The enum
   */
  def readerLowercaseOnly[A <: EnumEntry](`enum`: Enum[A]): BsonDecoder[A] = {
    case BString(s) => Try(`enum`.withNameLowercaseOnly(s))
    case _          => Failure(new RuntimeException("String value expected"))
  }

  /**
   * Returns a BSONReader for a given enum [[Enum]] transformed to upper case
   *
   * @param `enum`
   *   The enum
   */
  def readerUppercaseOnly[A <: EnumEntry](`enum`: Enum[A]): BsonDecoder[A] = {
    case BString(s) => Try(`enum`.withNameUppercaseOnly(s))
    case _          => Failure(new RuntimeException("String value expected"))
  }

  /**
   * Returns a BSONWriter for a given enum [[Enum]]
   */
  def writer[A <: EnumEntry](`enum`: Enum[A]): BsonEncoder[A] =
    (value: A) => BsonString(value.entryName)

  /**
   * Returns a BSONWriter for a given enum [[Enum]], outputting the value as lower case
   */
  def writerLowercase[A <: EnumEntry](`enum`: Enum[A]): BsonEncoder[A] =
    (value: A) => BsonString(value.entryName.toLowerCase())

  /**
   * Returns a BSONWriter for a given enum [[Enum]], outputting the value as upper case
   */
  def writerUppercase[A <: EnumEntry](`enum`: Enum[A]): BsonEncoder[A] =
    (value: A) => BsonString(value.entryName.toUpperCase())
}
