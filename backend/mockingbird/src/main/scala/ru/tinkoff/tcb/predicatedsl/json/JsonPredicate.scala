package ru.tinkoff.tcb.predicatedsl.json

import cats.data.NonEmptyList
import cats.data.Validated
import cats.data.ValidatedNel
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import org.bson.BsonInvalidOperationException
import sttp.tapir.Schema

import ru.tinkoff.tcb.bson.BsonDecoder
import ru.tinkoff.tcb.bson.BsonEncoder
import ru.tinkoff.tcb.circe.bson.*
import ru.tinkoff.tcb.generic.RootOptionFields
import ru.tinkoff.tcb.instances.jsonNumber.jsonNumberOrdering.mkOrderingOps
import ru.tinkoff.tcb.instances.predicate.and.*
import ru.tinkoff.tcb.predicatedsl.JSpecificationError
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.predicatedsl.Keyword.*
import ru.tinkoff.tcb.predicatedsl.PredicateConstructionError
import ru.tinkoff.tcb.protocol.bson.*
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.circe.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic

abstract class JsonPredicate extends (Json => Boolean) {
  def definition: JsonPredicate.Spec
  override def apply(json: Json): Boolean

  override def hashCode(): Int = definition.hashCode()

  override def equals(obj: Any): Boolean = obj match {
    case jp: JsonPredicate => jp.definition == definition
    case _                 => false
  }
}

object JsonPredicate {
  type Spec = Map[JsonOptic, Map[Keyword.Json, Json]]

  implicit val jsonPredicateDecoder: Decoder[JsonPredicate] =
    Decoder[Spec].emap(apply(_).toEither.leftMap(_.toList.mkString(", ")))

  implicit val jsonPredicateEncoder: Encoder[JsonPredicate] =
    Encoder[Spec].contramap(_.definition)

  implicit val jsonPredicateSchema: Schema[JsonPredicate] =
    implicitly[Schema[Spec]].as[JsonPredicate]

  implicit val jsonPredicateBsonDecoder: BsonDecoder[JsonPredicate] =
    BsonDecoder[Spec].afterReadTry(
      apply(_).toEither.leftMap(errs => new BsonInvalidOperationException(errs.toList.mkString(", "))).toTry
    )

  implicit val jsonPredicateBsonEncoder: BsonEncoder[JsonPredicate] =
    BsonEncoder[Spec].beforeWrite(_.definition)

  implicit val jsonPredicateRootOptionFields: RootOptionFields[JsonPredicate] =
    RootOptionFields.mk[JsonPredicate](RootOptionFields[Spec].fields, RootOptionFields[Spec].isOptionItself)

  def apply(
      defn: Spec
  ): ValidatedNel[PredicateConstructionError, JsonPredicate] =
    defn.toVector
      .map { case (optic, spec) =>
        spec.toVector
          .map(mkPredicate.tupled)
          .reduceOption(_ |+| _)
          .getOrElse(Validated.validNel((_: Json) => true))
          .map(optic.get andThen _)
          .leftMap(errors => NonEmptyList.one(JSpecificationError(optic, errors)))
      }
      .reduceOption(_ |+| _)
      .getOrElse(Validated.valid((_: Json) => true))
      .map(f =>
        new JsonPredicate {
          override def definition: Spec = defn

          override def apply(json: Json): Boolean = f(json)
        }
      )

  private val mkPredicate: (Keyword.Json, Json) => ValidatedNel[(Keyword, Json), Json => Boolean] =
    (kwd, jv) =>
      (kwd, jv) match {
        case (Equals, value)             => Validated.valid(_ == value)
        case (NotEq, value)              => Validated.valid(_ != value)
        case (Greater, JsonNumber(jnum)) => Validated.valid(_.asNumber.exists(_ > jnum))
        case (Gte, JsonNumber(jnum))     => Validated.valid(_.asNumber.exists(_ >= jnum))
        case (Less, JsonNumber(jnum))    => Validated.valid(_.asNumber.exists(_ < jnum))
        case (Lte, JsonNumber(jnum))     => Validated.valid(_.asNumber.exists(_ <= jnum))
        case (Rx, JsonString(str))       => Validated.valid(_.asString.exists(_.matches(str)))
        case (Size, JsonNumber(jnum)) if jnum.toInt.isDefined =>
          Validated.valid {
            case JsonString(str) => str.length == jnum.toInt.get
            case JsonVector(vec) => vec.length == jnum.toInt.get
            case _               => false
          }
        case (Exists, JsonBoolean(true))  => Validated.valid(!_.isNull)
        case (Exists, JsonBoolean(false)) => Validated.valid(_.isNull)
        case (In, JsonVector(vec))        => Validated.valid(vec.contains)
        case (NotIn, JsonVector(vec))     => Validated.valid(!vec.contains(_))
        case (AllIn, JsonVector(vec))     => Validated.valid(_.asArray.exists(vx => vec.forall(vx.contains)))
        case (kwd, j)                     => Validated.invalidNel(kwd -> j)
      }
}
