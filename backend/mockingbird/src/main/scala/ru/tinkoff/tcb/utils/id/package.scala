package ru.tinkoff.tcb.utils

import java.util.UUID
import scala.util.Success

import io.circe.KeyDecoder
import io.circe.KeyEncoder
import sttp.tapir.Codec
import sttp.tapir.CodecFormat

import ru.tinkoff.tcb.bson.BsonKeyDecoder
import ru.tinkoff.tcb.bson.BsonKeyEncoder

package object id {
  type SID[T] = SID.Aux[T]

  object SID extends IDCompanion[String] {
    def random[T]: SID[T] = SID(UUID.randomUUID().toString)

    implicit def keyEncoderForSID[T]: KeyEncoder[SID[T]] = identity[SID[T]] _
    implicit def keyDecoderForSID[T]: KeyDecoder[SID[T]] = (key: String) => Some(SID(key))

    implicit def bsonKeyEncoderForSID[T]: BsonKeyEncoder[SID[T]] = (t: SID[T]) => t
    implicit def bsonKeyDecoderForSID[T]: BsonKeyDecoder[SID[T]] = (value: String) => Success(apply(value))

    implicit def codecForSID[T]: Codec[String, SID[T], CodecFormat.TextPlain] =
      Codec.string.map(SID[T])(identity)
  }

  type ID[T] = ID.Aux[T]

  object ID extends IDCompanion[Int]

  type LID[T] = LID.Aux[T]

  object LID extends IDCompanion[Long]
}
