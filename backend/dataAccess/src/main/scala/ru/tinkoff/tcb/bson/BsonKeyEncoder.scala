package ru.tinkoff.tcb.bson

import scala.annotation.implicitNotFound

import simulacrum.typeclass

@implicitNotFound("Could not find an instance of BsonKeyEncoder for ${T}")
@typeclass
trait BsonKeyEncoder[T] extends Serializable {
  def encode(t: T): String

  def beforeWrite[H](f: H => T): BsonKeyEncoder[H] =
    (value: H) => this.encode(f(value))
}

object BsonKeyEncoder {
  /* ======================================================================== */
  /* THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      */
  /* ======================================================================== */

  /**
   * Summon an instance of [[BsonKeyEncoder]] for `T`.
   */
  @inline def apply[T](implicit instance: BsonKeyEncoder[T]): BsonKeyEncoder[T] = instance

  object ops {
    implicit def toAllBsonKeyEncoderOps[T](target: T)(implicit tc: BsonKeyEncoder[T]): AllOps[T] {
      type TypeClassType = BsonKeyEncoder[T]
    } = new AllOps[T] {
      type TypeClassType = BsonKeyEncoder[T]
      val self: T                          = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  trait Ops[T] extends Serializable {
    type TypeClassType <: BsonKeyEncoder[T]
    def self: T
    val typeClassInstance: TypeClassType
    def encode: String = typeClassInstance.encode(self)
  }
  trait AllOps[T] extends Ops[T]
  trait ToBsonKeyEncoderOps extends Serializable {
    implicit def toBsonKeyEncoderOps[T](target: T)(implicit tc: BsonKeyEncoder[T]): Ops[T] {
      type TypeClassType = BsonKeyEncoder[T]
    } = new Ops[T] {
      type TypeClassType = BsonKeyEncoder[T]
      val self: T                          = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToBsonKeyEncoderOps

  /* ======================================================================== */
  /* END OF SIMULACRUM-MANAGED CODE                                           */
  /* ======================================================================== */

}
