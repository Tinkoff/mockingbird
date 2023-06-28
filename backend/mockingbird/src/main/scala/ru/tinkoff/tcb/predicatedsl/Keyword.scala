package ru.tinkoff.tcb.predicatedsl

import scala.util.Try

import enumeratum.values.StringCirceEnum
import enumeratum.values.StringEnum
import enumeratum.values.StringEnumEntry
import io.circe.Decoder
import io.circe.Encoder
import io.circe.KeyDecoder
import io.circe.KeyEncoder
import sttp.tapir.Schema
import sttp.tapir.codec.enumeratum.TapirCodecEnumeratum

import ru.tinkoff.tcb.bson.BsonDecoder
import ru.tinkoff.tcb.bson.BsonEncoder
import ru.tinkoff.tcb.bson.BsonKeyDecoder
import ru.tinkoff.tcb.bson.BsonKeyEncoder
import ru.tinkoff.tcb.bson.enumeratum.values.StringBsonValueEnum

sealed abstract class Keyword(val value: String, val bsonKey: String) extends StringEnumEntry
sealed trait FormKeyword
sealed trait JsonKeyword extends FormKeyword
sealed trait XmlKeyword
object Keyword
    extends StringEnum[Keyword]
    with StringCirceEnum[Keyword]
    with StringBsonValueEnum[Keyword]
    with TapirCodecEnumeratum {
  case object Equals extends Keyword("==", "$eq") with JsonKeyword with XmlKeyword with FormKeyword
  case object NotEq extends Keyword("!=", "$ne") with JsonKeyword with XmlKeyword with FormKeyword
  case object Greater extends Keyword(">", "$gt") with JsonKeyword with XmlKeyword
  case object Gte extends Keyword(">=", "$gte") with JsonKeyword with XmlKeyword
  case object Less extends Keyword("<", "$lt") with JsonKeyword with XmlKeyword
  case object Lte extends Keyword("<=", "$lte") with JsonKeyword with XmlKeyword
  case object Rx extends Keyword("~=", "$regex") with JsonKeyword with XmlKeyword with FormKeyword
  case object Size extends Keyword("size", "$size") with JsonKeyword with XmlKeyword with FormKeyword
  case object Exists extends Keyword("exists", "$exists") with JsonKeyword with XmlKeyword
  case object In extends Keyword("[_]", "$in") with JsonKeyword with FormKeyword
  case object NotIn extends Keyword("![_]", "$nin") with JsonKeyword with FormKeyword
  case object AllIn extends Keyword("&[_]", "$all") with JsonKeyword with FormKeyword
  case object Cdata extends Keyword("cdata", "") with XmlKeyword
  case object JCdata extends Keyword("jcdata", "") with XmlKeyword
  case object XCdata extends Keyword("xcdata", "") with XmlKeyword

  val values = findValues

  // JsonKeyword
  type Json = Keyword & JsonKeyword

  implicit val jkDecoder: Decoder[Json] = circeDecoder.emapTry(kw => Try(kw.asInstanceOf[Json]))
  implicit val jkKeyDecoder: KeyDecoder[Json] =
    KeyDecoder.instance(key => circeKeyDecoder(key).flatMap(kw => Try(kw.asInstanceOf[Json]).toOption))
  implicit val jkEncoder: Encoder[Json]               = circeEncoder.contramap(identity)
  implicit val jkKeyEncoder: KeyEncoder[Json]         = KeyEncoder.instance(jk => circeKeyEncoder(jk))
  implicit val jkBsonDecoder: BsonDecoder[Json]       = bsonDecoder.afterReadTry(kw => Try(kw.asInstanceOf[Json]))
  implicit val jkBsonKeyDecoder: BsonKeyDecoder[Json] = bsonKeyDecoder.emapTry(kw => Try(kw.asInstanceOf[Json]))
  implicit val jkBsonEncoder: BsonEncoder[Json]       = bsonEncoder.beforeWrite(identity)
  implicit val jkBsonKeyEncoder: BsonKeyEncoder[Json] = bsonKeyEncoder.beforeWrite(identity)
  implicit val jsSchema: Schema[Json]                 = schemaForStringEnumEntry[Keyword].as[Json]
  // JsonKeyword

  // XmlKeyword
  type Xml = Keyword & XmlKeyword

  implicit val xkDecoder: Decoder[Xml] = circeDecoder.emapTry(kw => Try(kw.asInstanceOf[Xml]))
  implicit val xkKeyDecoder: KeyDecoder[Xml] =
    KeyDecoder.instance(key => circeKeyDecoder(key).flatMap(kw => Try(kw.asInstanceOf[Xml]).toOption))
  implicit val xkEncoder: Encoder[Xml]               = circeEncoder.contramap(identity)
  implicit val xkKeyEncoder: KeyEncoder[Xml]         = KeyEncoder.instance(jk => circeKeyEncoder(jk))
  implicit val xkBsonDecoder: BsonDecoder[Xml]       = bsonDecoder.afterReadTry(kw => Try(kw.asInstanceOf[Xml]))
  implicit val xkBsonKeyDecoder: BsonKeyDecoder[Xml] = bsonKeyDecoder.emapTry(kw => Try(kw.asInstanceOf[Xml]))
  implicit val xkBsonEncoder: BsonEncoder[Xml]       = bsonEncoder.beforeWrite(identity)
  implicit val xkBsonKeyEncoder: BsonKeyEncoder[Xml] = bsonKeyEncoder.beforeWrite(identity)
  implicit val xkSchema: Schema[Xml]                 = schemaForStringEnumEntry[Keyword].as[Xml]
  // XmlKeyword

  // StringKeyword
  type Form = Keyword & FormKeyword

  implicit val fkDecoder: Decoder[Form] = circeDecoder.emapTry(kw => Try(kw.asInstanceOf[Form]))
  implicit val fkKeyDecoder: KeyDecoder[Form] =
    KeyDecoder.instance(key => circeKeyDecoder(key).flatMap(kw => Try(kw.asInstanceOf[Form]).toOption))
  implicit val fkEncoder: Encoder[Form]               = circeEncoder.contramap(identity)
  implicit val fkKeyEncoder: KeyEncoder[Form]         = KeyEncoder.instance(fk => circeKeyEncoder(fk))
  implicit val fkBsonDecoder: BsonDecoder[Form]       = bsonDecoder.afterReadTry(kw => Try(kw.asInstanceOf[Form]))
  implicit val fkBsonKeyDecoder: BsonKeyDecoder[Form] = bsonKeyDecoder.emapTry(kw => Try(kw.asInstanceOf[Form]))
  implicit val fkBsonEncoder: BsonEncoder[Form]       = bsonEncoder.beforeWrite(identity)
  implicit val fkBsonKeyEncoder: BsonKeyEncoder[Form] = bsonKeyEncoder.beforeWrite(identity)
  implicit val fkSchema: Schema[Form]                 = schemaForStringEnumEntry[Keyword].as[Form]
  // StringKeyword
}
