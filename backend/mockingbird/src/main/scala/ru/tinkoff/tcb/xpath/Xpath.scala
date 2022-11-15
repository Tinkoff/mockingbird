package ru.tinkoff.tcb.xpath

import scala.util.Try

import io.circe.Decoder
import io.circe.Encoder
import io.circe.KeyDecoder
import io.circe.KeyEncoder
import kantan.xpath.XPathExpression
import org.mongodb.scala.bson.BsonString
import sttp.tapir.Schema

import ru.tinkoff.tcb.bson.BsonDecoder
import ru.tinkoff.tcb.bson.BsonEncoder
import ru.tinkoff.tcb.bson.BsonKeyDecoder
import ru.tinkoff.tcb.bson.BsonKeyEncoder

final case class Xpath(raw: String, toXPathExpr: XPathExpression) {
  override def toString: String = raw
}

object Xpath {
  def fromString(pathStr: String): Try[Xpath] =
    Try(xPathFactory.newXPath().compile(pathStr)).map(Xpath(pathStr, _))

  def unapply(str: String): Option[Xpath] =
    fromString(str).toOption

  implicit val xpathDecoder: Decoder[Xpath] =
    Decoder.decodeString.emapTry(fromString)

  implicit val xpathEncoder: Encoder[Xpath] =
    Encoder.encodeString.contramap(_.raw)

  implicit val xpathKeyDecoder: KeyDecoder[Xpath] = (key: String) => fromString(key).toOption

  implicit val xpathKeyEncoder: KeyEncoder[Xpath] = _.raw

  implicit val xpathBsonDecoder: BsonDecoder[Xpath] =
    BsonDecoder[String].afterReadTry(fromString)

  implicit val xpathBsonEncoder: BsonEncoder[Xpath] =
    BsonEncoder[String].beforeWrite(_.raw)

  implicit val xpathBsonKeyDecoder: BsonKeyDecoder[Xpath] =
    (value: String) => xpathBsonDecoder.fromBson(BsonString(value))

  implicit val xpathBsonKeyEncoder: BsonKeyEncoder[Xpath] = _.raw

  implicit val xpathSchema: Schema[Xpath] =
    Schema.schemaForString.as[Xpath]

  implicit val sxpathSchema: Schema[SXpath] =
    Schema.schemaForString.as[SXpath]
}
