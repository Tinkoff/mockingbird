package ru.tinkoff.tcb.bson

import scala.annotation.implicitNotFound
import scala.util.Try

import org.bson.BsonInvalidOperationException
import org.mongodb.scala.bson.*
import simulacrum.typeclass

@implicitNotFound("Could not find an instance of BsonDecoder for ${T}")
@typeclass
trait BsonDecoder[T] extends Serializable {
  def fromBson(value: BsonValue): Try[T]

  def afterRead[U](f: T => U): BsonDecoder[U] =
    (value: BsonValue) => fromBson(value).map(f)

  def afterReadTry[U](f: T => Try[U]): BsonDecoder[U] =
    (value: BsonValue) => fromBson(value).flatMap(f)
}

object BsonDecoder {
  def ofDocument[T](f: BsonDocument => Try[T]): BsonDecoder[T] =
    (value: BsonValue) => Try(value.asDocument()).flatMap(f)

  def ofArray[T](f: BsonArray => Try[T]): BsonDecoder[T] =
    (value: BsonValue) => Try(value.asArray()).flatMap(f)

  def partial[T](pf: PartialFunction[BsonValue, T]): BsonDecoder[T] =
    (value: BsonValue) =>
      Try(
        pf.applyOrElse[BsonValue, T](
          value,
          bv => throw new BsonInvalidOperationException(s"Can't decode $bv")
        )
      )

  /* ======================================================================== */
  /* THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      */
  /* ======================================================================== */

  /**
   * Summon an instance of [[BsonDecoder]] for `T`.
   */
  @inline def apply[T](implicit instance: BsonDecoder[T]): BsonDecoder[T] = instance

  object ops {
    implicit def toAllBsonDecoderOps[T](target: T)(implicit tc: BsonDecoder[T]): AllOps[T] {
      type TypeClassType = BsonDecoder[T]
    } = new AllOps[T] {
      type TypeClassType = BsonDecoder[T]
      val self: T                          = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  trait Ops[T] extends Serializable {
    type TypeClassType <: BsonDecoder[T]
    def self: T
    val typeClassInstance: TypeClassType
  }
  trait AllOps[T] extends Ops[T]
  trait ToBsonDecoderOps extends Serializable {
    implicit def toBsonDecoderOps[T](target: T)(implicit tc: BsonDecoder[T]): Ops[T] {
      type TypeClassType = BsonDecoder[T]
    } = new Ops[T] {
      type TypeClassType = BsonDecoder[T]
      val self: T                          = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToBsonDecoderOps

  /* ======================================================================== */
  /* END OF SIMULACRUM-MANAGED CODE                                           */
  /* ======================================================================== */

}
