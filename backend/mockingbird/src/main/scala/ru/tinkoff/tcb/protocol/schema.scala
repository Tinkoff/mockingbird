package ru.tinkoff.tcb.protocol

import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration
import scala.util.Try
import scala.util.matching.Regex

import cats.data.NonEmptyVector
import io.circe.Json
import sttp.tapir.Schema
import sttp.tapir.SchemaType
import sttp.tapir.Validator

import ru.tinkoff.tcb.mockingbird.model.JsonResponse
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic

object schema {
  // https://github.com/softwaremill/tapir/blob/master/json/circe/src/main/scala/sttp/tapir/json/circe/TapirJsonCirce.scala#L45-L50
  implicit val schemaForCirceJson: Schema[Json] =
    Schema(
      SchemaType.SCoproduct(Nil, None)(_ => None),
      None
    )

  implicit val jsonResponseSchema: Schema[JsonResponse] =
    Schema(SchemaType.SProduct[JsonResponse](Nil))

  implicit val regexSchema: Schema[Regex] =
    Schema.schemaForString.as[Regex]

  implicit val jsonOpticSchema: Schema[JsonOptic] =
    Schema.schemaForString.as[JsonOptic]

  // https://github.com/scala/scala/blob/2.13.x/src/library/scala/concurrent/duration/Duration.scala#L82
  private val timeUnitLabels =
    "d day days h hour hours m min mins minute minutes s sec secs second seconds ms milli millis millisecond milliseconds µs micro micros microsecond microseconds ns nano nanos nanosecond nanoseconds"
  private val pattern = s"\\d+\\s?(${timeUnitLabels.replaceAll("\\s", "|")})"

  implicit val finiteDurationSchema: Schema[FiniteDuration] =
    Schema.schemaForString
      .validate(Validator.pattern(pattern))
      .map(s => Try(Duration(s).pipe(d => FiniteDuration(d._1, d._2))).toOption)(_.toString())

  implicit def mapSchema[K, V](implicit underlying: Schema[V]): Schema[Map[K, V]] =
    Schema(SchemaType.SOpenProduct(Nil, underlying)(_ => Map()))

  implicit def nonEmptyVectorSchema[T: Schema]: Schema[NonEmptyVector[T]] =
    Schema.schemaForIterable[T, Vector].as[NonEmptyVector[T]]
}
