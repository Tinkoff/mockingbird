package ru.tinkoff.tcb.bson.enumeratum

import enumeratum.*

sealed trait Dummy extends EnumEntry
object Dummy extends Enum[Dummy] with BsonEnum[Dummy] {
  case object A extends Dummy
  case object B extends Dummy
  case object C extends Dummy
  val values = findValues
}
