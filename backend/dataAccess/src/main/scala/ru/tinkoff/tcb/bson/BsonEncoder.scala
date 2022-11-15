package ru.tinkoff.tcb.bson

import scala.annotation.implicitNotFound

import org.mongodb.scala.bson.BsonValue
import simulacrum.op
import simulacrum.typeclass

@implicitNotFound("Could not find an instance of BsonEncoder for ${T}")
@typeclass
trait BsonEncoder[T] extends Serializable {
  @op("bson") def toBson(value: T): BsonValue

  def beforeWrite[U](f: U => T): BsonEncoder[U] =
    (u: U) => toBson(f(u))
}

object BsonEncoder {
  def constant[T](bv: BsonValue): BsonEncoder[T] =
    (_: T) => bv

  /* ======================================================================== */
  /* THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      */
  /* ======================================================================== */

  /**
   * Summon an instance of [[BsonEncoder]] for `T`.
   */
  @inline def apply[T](implicit instance: BsonEncoder[T]): BsonEncoder[T] = instance

  object ops {
    implicit def toAllBsonEncoderOps[T](target: T)(implicit tc: BsonEncoder[T]): AllOps[T] {
      type TypeClassType = BsonEncoder[T]
    } = new AllOps[T] {
      type TypeClassType = BsonEncoder[T]
      val self: T                          = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  trait Ops[T] extends Serializable {
    type TypeClassType <: BsonEncoder[T]
    def self: T
    val typeClassInstance: TypeClassType
    def bson: BsonValue = typeClassInstance.toBson(self)
  }
  trait AllOps[T] extends Ops[T]
  trait ToBsonEncoderOps extends Serializable {
    implicit def toBsonEncoderOps[T](target: T)(implicit tc: BsonEncoder[T]): Ops[T] {
      type TypeClassType = BsonEncoder[T]
    } = new Ops[T] {
      type TypeClassType = BsonEncoder[T]
      val self: T                          = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToBsonEncoderOps

  /* ======================================================================== */
  /* END OF SIMULACRUM-MANAGED CODE                                           */
  /* ======================================================================== */

}
