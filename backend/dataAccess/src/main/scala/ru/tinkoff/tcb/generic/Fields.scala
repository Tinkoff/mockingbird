package ru.tinkoff.tcb.generic

import java.time.Instant
import java.time.Year
import scala.annotation.implicitNotFound

import enumeratum.EnumEntry
import enumeratum.values.StringEnumEntry
import io.circe.Json
import magnolia1.*
import simulacrum.typeclass

@implicitNotFound("Could not find an instance of Fields for ${T}")
@typeclass
trait Fields[T] extends Serializable {
  def fields: List[String]
  override def toString: String = fields.mkString(", ")
}
object Fields {
  def mk[T](fs: List[String]): Fields[T] = new Fields[T] {
    override def fields: List[String] = fs
  }

  implicit val string: Fields[String]                            = mk(Nil)
  implicit val instant: Fields[Instant]                          = mk(Nil)
  implicit val year: Fields[Year]                                = mk(Nil)
  implicit val bd: Fields[BigDecimal]                            = mk(Nil)
  implicit val js: Fields[Json]                                  = mk(Nil)
  implicit def anyVal[T <: AnyVal]: Fields[T]                    = mk(Nil)
  implicit def opt[T](implicit tf: Fields[T]): Fields[Option[T]] = mk(tf.fields)
  implicit def seq[T](implicit tf: Fields[T]): Fields[Seq[T]]    = mk(tf.fields)
  implicit def set[T](implicit tf: Fields[T]): Fields[Set[T]]    = mk(tf.fields)
  implicit def map[K, V]: Fields[K Map V]                        = mk(Nil)
  implicit def enumEntry[T <: EnumEntry]: Fields[T]              = mk(Nil)
  implicit def strEnum[T <: StringEnumEntry]: Fields[T]          = mk(Nil)

  type Typeclass[T] = Fields[T]

  def join[T](caseClass: CaseClass[Typeclass, T]): Typeclass[T] =
    mk(
      caseClass.parameters
        .foldLeft(List.newBuilder[String])((acc, fld) =>
          if (fld.typeclass.fields.isEmpty) acc += fld.label
          else acc ++= fld.typeclass.fields.map(f => s"${fld.label}.$f")
        )
        .result()
    )

  def split[T](sealedTrait: SealedTrait[Typeclass, T]): Typeclass[T] = mk(Nil)

  implicit def genFields[T]: Typeclass[T] = macro Magnolia.gen[T]

  /* ======================================================================== */
  /* THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      */
  /* ======================================================================== */

  /**
   * Summon an instance of [[Fields]] for `T`.
   */
  @inline def apply[T](implicit instance: Fields[T]): Fields[T] = instance

  object ops {
    implicit def toAllFieldsOps[T](target: T)(implicit tc: Fields[T]): AllOps[T] {
      type TypeClassType = Fields[T]
    } = new AllOps[T] {
      type TypeClassType = Fields[T]
      val self: T                          = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  trait Ops[T] extends Serializable {
    type TypeClassType <: Fields[T]
    def self: T
    val typeClassInstance: TypeClassType
  }
  trait AllOps[T] extends Ops[T]
  trait ToFieldsOps extends Serializable {
    implicit def toFieldsOps[T](target: T)(implicit tc: Fields[T]): Ops[T] {
      type TypeClassType = Fields[T]
    } = new Ops[T] {
      type TypeClassType = Fields[T]
      val self: T                          = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToFieldsOps

  /* ======================================================================== */
  /* END OF SIMULACRUM-MANAGED CODE                                           */
  /* ======================================================================== */

}
