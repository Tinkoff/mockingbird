package ru.tinkoff.tcb.generic

import java.time.Instant
import java.time.Year
import scala.annotation.implicitNotFound

import magnolia1.*
import simulacrum.typeclass

@implicitNotFound("Could not find an instance of RootOptionFields for ${T}")
@typeclass
trait RootOptionFields[T] extends Serializable {
  def fields: Set[String]
  def isOptionItself: Boolean
  override def toString: String = fields.mkString(", ")
}

object RootOptionFields {
  def mk[T](fs: Set[String], isOption: Boolean = false): RootOptionFields[T] =
    new RootOptionFields[T] {
      override def fields: Set[String]     = fs
      override def isOptionItself: Boolean = isOption
    }

  implicit val string: RootOptionFields[String]         = mk(Set.empty)
  implicit val instant: RootOptionFields[Instant]       = mk(Set.empty)
  implicit val year: RootOptionFields[Year]             = mk(Set.empty)
  implicit def anyVal[T <: AnyVal]: RootOptionFields[T] = mk(Set.empty)
  implicit def opt[T]: RootOptionFields[Option[T]]      = mk(Set.empty, isOption = true)
  implicit def seq[T]: RootOptionFields[Seq[T]]         = mk(Set.empty)
  implicit def map[K, V]: RootOptionFields[K Map V]     = mk(Set.empty)
  implicit def vector[T]: RootOptionFields[Vector[T]]   = mk(Set.empty)
  implicit def list[T]: RootOptionFields[List[T]]       = mk(Set.empty)

  type Typeclass[T] = RootOptionFields[T]

  def join[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] =
    mk(
      caseClass.parameters
        .foldLeft(Set.newBuilder[String])((acc, fld) =>
          if (fld.typeclass.isOptionItself) acc += fld.label
          else acc
        )
        .result()
    )

  def split[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = mk(Set.empty)

  implicit def genRootOptionFields[T]: Typeclass[T] = macro Magnolia.gen[T]

  /* ======================================================================== */
  /* THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      */
  /* ======================================================================== */

  /**
   * Summon an instance of [[RootOptionFields]] for `T`.
   */
  @inline def apply[T](implicit instance: RootOptionFields[T]): RootOptionFields[T] = instance

  object ops {
    implicit def toAllRootOptionFieldsOps[T](target: T)(implicit tc: RootOptionFields[T]): AllOps[T] {
      type TypeClassType = RootOptionFields[T]
    } = new AllOps[T] {
      type TypeClassType = RootOptionFields[T]
      val self: T                          = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  trait Ops[T] extends Serializable {
    type TypeClassType <: RootOptionFields[T]
    def self: T
    val typeClassInstance: TypeClassType
  }
  trait AllOps[T] extends Ops[T]
  trait ToRootOptionFieldsOps extends Serializable {
    implicit def toRootOptionFieldsOps[T](target: T)(implicit tc: RootOptionFields[T]): Ops[T] {
      type TypeClassType = RootOptionFields[T]
    } = new Ops[T] {
      type TypeClassType = RootOptionFields[T]
      val self: T                          = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToRootOptionFieldsOps

  /* ======================================================================== */
  /* END OF SIMULACRUM-MANAGED CODE                                           */
  /* ======================================================================== */

}
