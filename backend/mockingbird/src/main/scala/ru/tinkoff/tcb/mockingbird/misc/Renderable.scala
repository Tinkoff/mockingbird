package ru.tinkoff.tcb.mockingbird.misc

import scala.annotation.implicitNotFound

import io.circe.Json
import io.circe.syntax.*
import simulacrum.*

import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic

@implicitNotFound("Could not find an instance of Renderable for ${T}")
@typeclass
trait Renderable[T] extends Serializable {
  @op("renderJson") def renderJson(r: T): Json

  @op("fill") def fill[S](r: T, values: S)(implicit subst: Substitute[Json, S]): T

  @op("withPrefix") def withPrefix(r: T, prefix: String): T
}

object Renderable {
  import ops.*

  implicit val updateSpecRenderable: Renderable[Map[JsonOptic, Json]] =
    new Renderable[Map[JsonOptic, Json]] {
      override def renderJson(spec: Map[JsonOptic, Json]): Json =
        Json.fromFields(spec.map { case (k, v) => k.path -> v })

      override def fill[S](spec: Map[JsonOptic, Json], values: S)(implicit
          subst: Substitute[Json, S]
      ): Map[JsonOptic, Json] =
        spec.view.mapValues(subst.substitute(_, values)).toMap

      override def withPrefix(spec: Map[JsonOptic, Json], prefix: String): Map[JsonOptic, Json] = {
        val pjo = JsonOptic.fromPathString(prefix)

        spec.map { case (k, v) => pjo \\ k -> v }
      }
    }

  implicit val querySpecRenderable: Renderable[Map[JsonOptic, Map[Keyword.Json, Json]]] =
    new Renderable[Map[JsonOptic, Map[Keyword.Json, Json]]] {
      override def renderJson(spec: Map[JsonOptic, Map[Keyword.Json, Json]]): Json =
        spec.view
          .mapValues { predicates =>
            Json.fromFields(
              predicates.map { case (pk, pval) =>
                pk.bsonKey := pval
              }
            )
          }
          .toMap
          .renderJson

      override def fill[S](spec: Map[JsonOptic, Map[Keyword.Json, Json]], values: S)(implicit
          subst: Substitute[Json, S]
      ): Map[JsonOptic, Map[Keyword.Json, Json]] =
        spec.view.mapValues(_.view.mapValues(subst.substitute(_, values)).toMap).toMap

      override def withPrefix(
          spec: Map[JsonOptic, Map[Keyword.Json, Json]],
          prefix: String
      ): Map[JsonOptic, Map[Keyword.Json, Json]] = {
        val pjo = JsonOptic.fromPathString(prefix)

        spec.map { case (k, v) => pjo \\ k -> v }
      }
    }

  /* ======================================================================== */
  /* THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      */
  /* ======================================================================== */

  /**
   * Summon an instance of [[Renderable]] for `T`.
   */
  @inline def apply[T](implicit instance: Renderable[T]): Renderable[T] = instance

  object ops {
    implicit def toAllRenderableOps[T](target: T)(implicit tc: Renderable[T]): AllOps[T] {
      type TypeClassType = Renderable[T]
    } = new AllOps[T] {
      type TypeClassType = Renderable[T]
      val self: T                          = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  trait Ops[T] extends Serializable {
    type TypeClassType <: Renderable[T]
    def self: T
    val typeClassInstance: TypeClassType
    def renderJson: Json                                           = typeClassInstance.renderJson(self)
    def fill[A](values: A)(implicit subst: Substitute[Json, A]): T = typeClassInstance.fill[A](self, values)(subst)
    def withPrefix(prefix: String): T                              = typeClassInstance.withPrefix(self, prefix)
  }
  trait AllOps[T] extends Ops[T]
  trait ToRenderableOps extends Serializable {
    implicit def toRenderableOps[T](target: T)(implicit tc: Renderable[T]): Ops[T] {
      type TypeClassType = Renderable[T]
    } = new Ops[T] {
      type TypeClassType = Renderable[T]
      val self: T                          = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToRenderableOps

  /* ======================================================================== */
  /* END OF SIMULACRUM-MANAGED CODE                                           */
  /* ======================================================================== */

}
