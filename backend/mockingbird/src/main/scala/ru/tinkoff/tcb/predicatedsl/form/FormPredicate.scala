package ru.tinkoff.tcb.predicatedsl.form

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
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.predicatedsl.PredicateConstructionError
import ru.tinkoff.tcb.predicatedsl.json.JsonPredicate
import ru.tinkoff.tcb.protocol.bson.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.circe.optics.JLens
import ru.tinkoff.tcb.utils.webform.toJson

abstract class FormPredicate extends (Map[String, List[String]] => Boolean) {
  def definition: FormPredicate.Spec
  override def apply(form: Map[String, List[String]]): Boolean

  override def hashCode(): Int = definition.hashCode()

  override def equals(obj: Any): Boolean = obj match {
    case sp: FormPredicate => sp.definition == definition
    case _                 => false
  }
}

object FormPredicate {
  type Spec = Map[String, Map[Keyword.Form, Json]]

  implicit val jsonPredicateDecoder: Decoder[FormPredicate] =
    Decoder[Spec].emap(apply(_).toEither.leftMap(_.toList.mkString(", ")))

  implicit val jsonPredicateEncoder: Encoder[FormPredicate] =
    Encoder[Spec].contramap(_.definition)

  implicit val jsonPredicateSchema: Schema[FormPredicate] =
    implicitly[Schema[Spec]].as[FormPredicate]

  implicit val jsonPredicateBsonDecoder: BsonDecoder[FormPredicate] =
    BsonDecoder[Spec].afterReadTry(
      apply(_).toEither.leftMap(errs => new BsonInvalidOperationException(errs.toList.mkString(", "))).toTry
    )

  implicit val jsonPredicateBsonEncoder: BsonEncoder[FormPredicate] =
    BsonEncoder[Spec].beforeWrite(_.definition)

  implicit val jsonPredicateRootOptionFields: RootOptionFields[FormPredicate] =
    RootOptionFields.mk[FormPredicate](RootOptionFields[Spec].fields, RootOptionFields[Spec].isOptionItself)

  def apply(defn: Spec): ValidatedNel[PredicateConstructionError, FormPredicate] =
    Validated
      .valid(defn)
      .map(_.map { case (key, value) =>
        (JLens \ key) -> value.asInstanceOf[Map[Keyword.Json, Json]]
      })
      .andThen(JsonPredicate(_))
      .map { jp =>
        new FormPredicate {
          override def definition: Spec = defn

          override def apply(form: Map[String, List[String]]): Boolean =
            jp(toJson(form))
        }
      }
}
