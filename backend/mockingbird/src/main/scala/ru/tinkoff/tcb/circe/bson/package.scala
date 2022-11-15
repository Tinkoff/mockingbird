package ru.tinkoff.tcb.circe

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import alleycats.std.map.*
import io.circe.Json
import io.circe.JsonNumber
import io.circe.JsonObject
import org.bson.BsonInvalidOperationException
import org.bson.types.Decimal128
import org.mongodb.scala.bson.*

import ru.tinkoff.tcb.bson.*
import ru.tinkoff.tcb.utils.circe.JsonDocument

package object bson {
  private[this] def readerFailure(value: BsonValue): BsonInvalidOperationException =
    new BsonInvalidOperationException(
      s"Cannot convert $value: ${value.getClass} to io.circe.Json with io.ru.tinkoff.tcb.bson",
    )

  final def bsonToJson(bson: BsonValue): Either[Throwable, Json] = (bson: @unchecked) match {
    case BBoolean(value) => Right(Json.fromBoolean(value))
    case BString(value)  => Right(Json.fromString(value))
    case BDouble(value) =>
      Json.fromDouble(value) match {
        case Some(json) => Right(json)
        case None       => Left(readerFailure(bson))
      }
    case BLong(value)    => Right(Json.fromLong(value))
    case BInt(value)     => Right(Json.fromInt(value))
    case BDecimal(value) => Right(Json.fromBigDecimal(value))
    case BArray(values)  => values.traverse(bsonToJson).map(Json.fromValues)
    case BDocument(values) =>
      values
        .traverse(bsonToJson)
        .map(Json.fromFields)
    case BDateTime(value) => Right(Json.obj("$date" -> Json.fromLong(value.toEpochMilli)))
    // case BSONTimestamp(value)       => Right(Json.fromLong(value))
    case BNull() | BUndef()          => Right(Json.Null)
    case BSymbol(value)              => Right(Json.fromString(value))
    case BJavaScript(value)          => Right(Json.fromString(value))
    case BScopedJavaScript(value, _) => Right(Json.fromString(value))
    // case BSONMaxKey                 => Left(readerFailure(bson))
    // case BSONMinKey                 => Left(readerFailure(bson))
    case BObjectId(value) => Right(Json.fromString(value.toString))
    // case BSONBinary(_)   => Left(readerFailure(bson))
    // case BSONRegex(_, _) => Left(readerFailure(bson))
  }

  private[this] lazy val jsonFolder: Json.Folder[Either[Throwable, BsonValue]] =
    new Json.Folder[Either[Throwable, BsonValue]] { self =>
      final val onNull: Either[Throwable, BsonValue]                    = Right(BsonNull())
      final def onBoolean(value: Boolean): Either[Throwable, BsonValue] = Right(BsonBoolean(value))
      final def onNumber(value: JsonNumber): Either[Throwable, BsonValue] = {
        val asDouble = value.toDouble

        if (java.lang.Double.compare(asDouble, -0.0) == 0) {
          Right(BsonDecimal128(Decimal128.NEGATIVE_ZERO))
        } else
          value.toLong match {
            case Some(n) => Right(BsonInt64(n))
            case None =>
              value.toBigDecimal match {
                case Some(n) =>
                  Try(BsonDecimal128(n)) match {
                    case Success(dec)   => Right(dec)
                    case Failure(error) => Left(error)
                  }
                case None =>
                  Try(BsonDecimal128(value.toString)) match {
                    case Success(dec)   => Right(dec)
                    case Failure(error) => Left(error)
                  }
              }
          }
      }
      final def onString(value: String): Either[Throwable, BsonValue] = Right(BsonString(value))
      final def onArray(value: Vector[Json]): Either[Throwable, BsonValue] =
        value
          .traverse(json => json.foldWith(self))
          .map(BsonArray.fromIterable(_))
      final def onObject(value: JsonObject): Either[Throwable, BsonValue] =
        (value.toVector)
          .traverse {
            case (key, JsonDocument(json)) if json.contains("$date") =>
              json("$date")
                .flatMap(_.asNumber)
                .flatMap(_.toLong)
                .map(key -> BsonDateTime(_)) match {
                case Some(bdt) => Right(bdt)
                case None =>
                  Left(
                    new BsonInvalidOperationException(
                      "Unable to convert JsonObject with $date to BSONDateTime, for key: %s"
                        .format(key)
                    )
                  )
              }
            case (key, json) => json.foldWith(self).map(key -> _)
          }
          .map(BsonDocument(_))
    }

  final def jsonToBson(json: Json): Either[Throwable, BsonValue] = json.foldWith(jsonFolder)

  implicit final lazy val jsonBsonReader: BsonDecoder[Json] =
    (value: BsonValue) => bsonToJson(value).toTry

  implicit final lazy val jsonBsonWriter: BsonEncoder[Json] =
    (value: Json) => jsonToBson(value).toTry.get
}
