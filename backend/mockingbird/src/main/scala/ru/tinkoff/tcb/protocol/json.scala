package ru.tinkoff.tcb.protocol

import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration
import scala.util.Try
import scala.util.matching.Regex

import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.KeyDecoder
import io.circe.KeyEncoder

import ru.tinkoff.tcb.bson.optics.BsonOptic
import ru.tinkoff.tcb.utils.circe.optics.JLens
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.utils.string.*

object json {
  implicit val bsonOpticEncoder: Encoder[BsonOptic] = (op: BsonOptic) => Json.fromString(op.path)
  implicit val bsonOpticDecoder: Decoder[BsonOptic] = Decoder.decodeString.map {
    BsonOptic.fromPathString
  }

  implicit val jsonOpticDecoder: Decoder[JsonOptic] =
    Decoder.decodeString.map(_.nonEmptyString.map(JsonOptic.fromPathString).getOrElse(JLens))

  implicit val jsonOpticKeyDecoder: KeyDecoder[JsonOptic] =
    KeyDecoder.decodeKeyString.map(_.nonEmptyString.map(JsonOptic.fromPathString).getOrElse(JLens))

  implicit val jsonOpticEncoder: Encoder[JsonOptic] =
    Encoder.encodeString.contramap(_.path)

  implicit val jsonOpticKeyEncoder: KeyEncoder[JsonOptic] =
    KeyEncoder.encodeKeyString.contramap(_.path)

  implicit val finiteDurationEncoder: Encoder[FiniteDuration] =
    Encoder.instance(value => Json.fromString(value.toString))

  implicit val finiteDurationDecoder: Decoder[FiniteDuration] = Decoder
    .instance(_.as[String])
    .emap { str =>
      Try {
        val d = Duration(str)
        FiniteDuration(d._1, d._2)
      }.toEither.left.map(_.getMessage)
    }

  implicit val regexEndoder: Encoder[Regex] =
    Encoder.instance(value => Json.fromString(value.regex))

  implicit val regexDecoder: Decoder[Regex] =
    Decoder.decodeString.map(new Regex(_))
}
