package ru.tinkoff.tcb.mockingbird.model

import enumeratum.*
import enumeratum.EnumEntry.Lowercase
import sttp.tapir.codec.enumeratum.TapirCodecEnumeratum

import ru.tinkoff.tcb.bson.enumeratum.BsonEnum

sealed trait CallbackResponseMode extends EnumEntry with Lowercase
object CallbackResponseMode
    extends Enum[CallbackResponseMode]
    with CirceEnum[CallbackResponseMode]
    with BsonEnum[CallbackResponseMode]
    with TapirCodecEnumeratum {
  case object Json extends CallbackResponseMode
  case object Xml extends CallbackResponseMode

  val values = findValues
}
