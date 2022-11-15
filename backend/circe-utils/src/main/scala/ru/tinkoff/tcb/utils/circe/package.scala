package ru.tinkoff.tcb.utils

import scala.util.control.TailCalls
import scala.util.control.TailCalls.TailRec

import cats.instances.vector.*
import io.circe.*
import io.circe.generic.extras.*

import ru.tinkoff.tcb.utils.circe.optics.JsonOptic

package object circe {
  object JsonString {
    def unapply(arg: Json): Option[String] = arg.asString
  }

  object JsonVector {
    def unapply(arg: Json): Option[Vector[Json]] = arg.asArray
  }

  object JsonBoolean {
    def unapply(arg: Json): Option[Boolean] = arg.asBoolean
  }

  object JsonNull {
    def unapply(arg: Json): Option[Unit] = arg.asNull
  }

  object JsonDocument {
    def unapply(arg: Json): Option[JsonObject] = arg.asObject
  }

  object JsonNumber {
    def unapply(arg: Json): Option[JsonNumber] = arg.asNumber
  }

  // by Travis Brown (https://stackoverflow.com/a/37619752)
  implicit class JsonObjectOps(private val jso: JsonObject) extends AnyVal {
    def transformObjectKeys(f: String => String): JsonObject =
      JsonObject.fromIterable(jso.toList.map { case (k, v) =>
        f(k) -> v
      })
  }

  private def merge(base: Json, patch: Json, arraySubvalues: Boolean): Json =
    (base, patch) match {
      case (JsonDocument(lhs), JsonDocument(rhs)) =>
        Json.fromJsonObject(lhs.toList.foldLeft(rhs) { case (acc, (key, value)) =>
          rhs(key).fold(acc.add(key, value))(r => acc.add(key, merge(value, r, false)))
        })
      case (JsonVector(baseArr), JsonVector(patchArr)) =>
        val mrgPair = (l: Json, r: Json) => merge(l, r, arraySubvalues = true)

        if (baseArr.length >= patchArr.length)
          Json.fromValues((baseArr zip patchArr).map(mrgPair.tupled))
        else Json.fromValues(baseArr.zipAll(patchArr, Json.Null, patchArr.last).map(mrgPair.tupled))
      case (p, JsonNull(())) if arraySubvalues => p
      case (_, p)                              => p
    }

  implicit class JsonOps(private val json: Json) extends AnyVal {
    def transformKeys(f: String => String): TailRec[Json] =
      json.arrayOrObject(
        TailCalls.done(json),
        arr => arr.traverse(j => TailCalls.tailcall(j.transformKeys(f))).map(Json.fromValues),
        _.transformObjectKeys(f)
          .traverse(obj => TailCalls.tailcall(obj.transformKeys(f)))
          .map(Json.fromJsonObject)
      )

    def camelizeKeys: Json =
      transformKeys(in => "_([a-z\\d])".r.replaceAllIn(in, _.group(1).toUpperCase)).result

    /**
     * Производит слияние двух json объектов
     *
     * json1 :+ json2
     *
     * В случае совпадения значений по определённому ключу приоритетными являются значения из json1
     */
    @inline def :+(other: Json): Json = merge(other, json, false)

    /**
     * Производит слияние двух json объектов
     *
     * json1 +: json2
     *
     * В случае совпадения значений по определённому ключу приоритетными являются значения из json2
     */
    @inline def +:(other: Json): Json = merge(other, json, false)

    def get(optic: JsonOptic): Json            = optic.get(json)
    def getOpt(optic: JsonOptic): Option[Json] = optic.getOpt(json)
  }

  implicit def eitherDecoder[A, B](implicit a: Decoder[A], b: Decoder[B]): Decoder[Either[A, B]] = {
    val l: Decoder[Either[A, B]] = a.map(Left.apply)
    val r: Decoder[Either[A, B]] = b.map(Right.apply)
    l or r
  }

  implicit def eitherEncoder[A, B](implicit a: Encoder[A], b: Encoder[B]): Encoder[Either[A, B]] = {
    case Left(va)  => a.apply(va)
    case Right(vb) => b.apply(vb)
  }

  object UpperCamelCaseConfiguration {
    private def pascalize(word: String): String =
      word
        .split("_")
        .map(s => s.capitalize)
        .mkString

    implicit val circeConfig: Configuration =
      Configuration(pascalize, identity, useDefaults = false, None)
  }
}
