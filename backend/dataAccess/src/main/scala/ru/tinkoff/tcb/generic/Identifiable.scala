package ru.tinkoff.tcb.generic

import scala.annotation.implicitNotFound

import simulacrum.typeclass

@implicitNotFound("Could not find an instance of Identifiable for ${T}")
@typeclass
trait Identifiable[T] extends Serializable {
  def getId(t: T): String
}

object Identifiable {
  /* ======================================================================== */
  /* THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      */
  /* ======================================================================== */

  /**
   * Summon an instance of [[Identifiable]] for `T`.
   */
  @inline def apply[T](implicit instance: Identifiable[T]): Identifiable[T] = instance

  object ops {
    implicit def toAllIdentifiableOps[T](target: T)(implicit tc: Identifiable[T]): AllOps[T] {
      type TypeClassType = Identifiable[T]
    } = new AllOps[T] {
      type TypeClassType = Identifiable[T]
      val self: T                          = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  trait Ops[T] extends Serializable {
    type TypeClassType <: Identifiable[T]
    def self: T
    val typeClassInstance: TypeClassType
    def getId: String = typeClassInstance.getId(self)
  }
  trait AllOps[T] extends Ops[T]
  trait ToIdentifiableOps extends Serializable {
    implicit def toIdentifiableOps[T](target: T)(implicit tc: Identifiable[T]): Ops[T] {
      type TypeClassType = Identifiable[T]
    } = new Ops[T] {
      type TypeClassType = Identifiable[T]
      val self: T                          = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToIdentifiableOps

  /* ======================================================================== */
  /* END OF SIMULACRUM-MANAGED CODE                                           */
  /* ======================================================================== */

}
