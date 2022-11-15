package ru.tinkoff.tcb.protocol

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import cats.data.NonEmptyVector
import eu.timepit.refined.api.RefType
import eu.timepit.refined.api.Validate
import org.bson.BsonInvalidOperationException
import org.mongodb.scala.bson.*

import ru.tinkoff.tcb.bson.*
import ru.tinkoff.tcb.utils.circe.optics.JLens
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.utils.string.*

object bson {
  implicit val jsonOpticBsonEncoder: BsonEncoder[JsonOptic] =
    (value: JsonOptic) => BsonString(value.path.replace('.', '⋮'))

  implicit val jsonOpticBsonDecoder: BsonDecoder[JsonOptic] =
    (value: BsonValue) =>
      Try(value.asString().getValue)
        .map(_.nonEmptyString.map(_.replace('⋮', '.')).map(JsonOptic.fromPathString).getOrElse(JLens))

  implicit val jsonOpticBsonKeyEncoder: BsonKeyEncoder[JsonOptic] = (j: JsonOptic) => j.path.replace('.', '⋮')

  implicit val jsonOpticBsonKeyDecoder: BsonKeyDecoder[JsonOptic] = (value: String) =>
    Try(value.nonEmptyString.map(_.replace('⋮', '.')).map(JsonOptic.fromPathString).getOrElse(JLens))

  implicit final def refinedBsonDecoder[T, P, F[_, _]](implicit
      underlying: BsonDecoder[T],
      validate: Validate[T, P],
      refType: RefType[F]
  ): BsonDecoder[F[T, P]] =
    BsonDecoder { c =>
      underlying.fromBson(c) match {
        case Success(t0) =>
          refType.refine(t0) match {
            case Left(err) => Failure(new BsonInvalidOperationException(err))
            case Right(v)  => Success(v)
          }
        case l @ Failure(_) => l.asInstanceOf[Failure[F[T, P]]]
      }
    }

  implicit final def refinedBsonEncoder[T, P, F[_, _]](implicit
      underlying: BsonEncoder[T],
      refType: RefType[F]
  ): BsonEncoder[F[T, P]] =
    underlying.beforeWrite(refType.unwrap)

  implicit final def refinedBsonKeyDecoder[T, P, F[_, _]](implicit
      underlying: BsonKeyDecoder[T],
      validate: Validate[T, P],
      refType: RefType[F]
  ): BsonKeyDecoder[F[T, P]] =
    (value: String) =>
      underlying.decode(value).flatMap { t0 =>
        refType.refine(t0) match {
          case Left(err) => Failure(new BsonInvalidOperationException(err))
          case Right(v)  => Success(v)
        }
      }

  implicit final def refinedBsonKeyEncoder[T, P, F[_, _]](implicit
      underlying: BsonKeyEncoder[T],
      refType: RefType[F]
  ): BsonKeyEncoder[F[T, P]] =
    underlying.beforeWrite(refType.unwrap)

  implicit final def nonEmptyVectorBsonEncoder[T: BsonEncoder]: BsonEncoder[NonEmptyVector[T]] =
    BsonEncoder[Vector[T]].beforeWrite(_.toVector)

  implicit final def nonEmptyVectorBsonDecoder[T: BsonDecoder]: BsonDecoder[NonEmptyVector[T]] =
    BsonDecoder[Vector[T]].afterReadTry(v => Try(NonEmptyVector.fromVectorUnsafe(v)))
}
