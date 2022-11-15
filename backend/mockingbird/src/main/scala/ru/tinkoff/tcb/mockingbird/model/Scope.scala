package ru.tinkoff.tcb.mockingbird.model

import enumeratum.*
import enumeratum.EnumEntry.Lowercase
import mouse.option.*
import sttp.tapir.codec.enumeratum.TapirCodecEnumeratum
import tofu.logging.Loggable

import ru.tinkoff.tcb.bson.*
import ru.tinkoff.tcb.bson.enumeratum.BsonEnum

sealed abstract class Scope(val priority: Int) extends EnumEntry with Lowercase

object Scope extends Enum[Scope] with BsonEnum[Scope] with TapirCodecEnumeratum with CirceEnum[Scope] {
  case object Persistent extends Scope(0)
  case object Ephemeral extends Scope(1)
  case object Countdown extends Scope(2)

  val values = findValues

  implicit val scopeLoggable: Loggable[Scope] = Loggable.stringValue.contramap(_.toString)

  implicit val scopeBsonEncoder: BsonEncoder[Scope] = intBsonEncoder.beforeWrite(_.priority)

  implicit val scopeBsonDecoder: BsonDecoder[Scope] =
    intBsonDecoder.afterReadTry(p => values.find(_.priority == p).toTry(new Exception(s"No Scope with priority $p")))
}
