package ru.tinkoff.tcb.bson

import scala.annotation.implicitNotFound
import scala.util.Try

import simulacrum.typeclass

@implicitNotFound("Could not find an instance of BsonKeyDecoder for ${T}")
@typeclass
trait BsonKeyDecoder[T] extends Serializable {
  def decode(value: String): Try[T]

  def emapTry[H](f: T => Try[H]): BsonKeyDecoder[H] =
    (value: String) => this.decode(value).flatMap(f)
}

object BsonKeyDecoder {
  /* ======================================================================== */
  /* THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      */
  /* ======================================================================== */

  /**
   * Summon an instance of [[BsonKeyDecoder]] for `T`.
   */
  @inline def apply[T](implicit instance: BsonKeyDecoder[T]): BsonKeyDecoder[T] = instance

  object ops {
    implicit def toAllBsonKeyDecoderOps[T](target: T)(implicit tc: BsonKeyDecoder[T]): AllOps[T] {
      type TypeClassType = BsonKeyDecoder[T]
    } = new AllOps[T] {
      type TypeClassType = BsonKeyDecoder[T]
      val self: T                          = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  trait Ops[T] extends Serializable {
    type TypeClassType <: BsonKeyDecoder[T]
    def self: T
    val typeClassInstance: TypeClassType
  }
  trait AllOps[T] extends Ops[T]
  trait ToBsonKeyDecoderOps extends Serializable {
    implicit def toBsonKeyDecoderOps[T](target: T)(implicit tc: BsonKeyDecoder[T]): Ops[T] {
      type TypeClassType = BsonKeyDecoder[T]
    } = new Ops[T] {
      type TypeClassType = BsonKeyDecoder[T]
      val self: T                          = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToBsonKeyDecoderOps

  /* ======================================================================== */
  /* END OF SIMULACRUM-MANAGED CODE                                           */
  /* ======================================================================== */

}
