package ru.tinkoff.tcb.mockingbird.model

import enumeratum.*
import enumeratum.EnumEntry.Uppercase
import sttp.tapir.codec.enumeratum.TapirCodecEnumeratum

import ru.tinkoff.tcb.bson.enumeratum.BsonEnum
import ru.tinkoff.tcb.mockingbird.config.FicusEnum

sealed trait HttpMethod extends EnumEntry with Uppercase
object HttpMethod
    extends Enum[HttpMethod]
    with BsonEnum[HttpMethod]
    with TapirCodecEnumeratum
    with CirceEnum[HttpMethod]
    with FicusEnum[HttpMethod] {
  case object Get extends HttpMethod
  case object Post extends HttpMethod
  case object Head extends HttpMethod
  case object Options extends HttpMethod
  case object Patch extends HttpMethod
  case object Put extends HttpMethod
  case object Delete extends HttpMethod

  val values = findValues
}
