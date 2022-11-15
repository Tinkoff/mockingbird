package ru.tinkoff.tcb.generic

import scala.annotation.implicitNotFound

import eu.timepit.refined.api.RefType
import shapeless.*
import shapeless.labelled.FieldType
import shapeless.ops.record.Selector

/*
  Witnesses, that all fields of `Projection` exists in `Source` with the same types and names
 */
@implicitNotFound("${Projection} is not a valid projection of ${Source}")
trait PropSubset[Projection, Source]

object PropSubset {
  def apply[P, S](implicit propSubset: PropSubset[P, S]): PropSubset[P, S] = propSubset

  private val anySubset = new PropSubset[Any, Any] {}

  implicit def identitySubset[T]: PropSubset[T, T] =
    anySubset.asInstanceOf[PropSubset[T, T]]

  implicit def optionSubset[P, S](implicit
      notEq: P =:!= S,
      ps: PropSubset[P, S]
  ): PropSubset[Option[P], Option[S]] =
    ps.asInstanceOf[PropSubset[Option[P], Option[S]]]

  implicit def mapSubset[K, P, S](implicit
      notEq: P =:!= S,
      ps: PropSubset[P, S]
  ): PropSubset[K Map P, K Map S] =
    ps.asInstanceOf[PropSubset[K Map P, K Map S]]

  implicit def setSubset[P, S](implicit
      notEq: P =:!= S,
      ps: PropSubset[P, S]
  ): PropSubset[Set[P], Set[S]] =
    ps.asInstanceOf[PropSubset[Set[P], Set[S]]]

  implicit def unrefinedSubset[P, S, R, F[_, _]](implicit
      refType: RefType[F],
      ps: PropSubset[P, S]
  ): PropSubset[F[P, R], S] = ps.asInstanceOf[PropSubset[F[P, R], S]]

  implicit def refinedSubset[P, S, R, F[_, _]](implicit
      notEq: P =:!= S,
      refType: RefType[F],
      ps: PropSubset[P, S]
  ): PropSubset[F[P, R], F[S, R]] = ps.asInstanceOf[PropSubset[F[P, R], F[S, R]]]

  implicit def hNilSubset[S <: HList]: PropSubset[HNil, S] =
    anySubset.asInstanceOf[PropSubset[HNil, S]]

  implicit def hListSubset[K <: Symbol, PH, PT <: HList, SF, S <: HList](implicit
      notEq: (FieldType[K, PH] :: PT) =:!= S,
      s: Selector.Aux[S, K, SF],
      ps0: Lazy[PropSubset[PH, SF]],
      psTail: PropSubset[PT, S]
  ): PropSubset[FieldType[K, PH] :: PT, S] =
    anySubset.asInstanceOf[PropSubset[FieldType[K, PH] :: PT, S]]

  implicit def genericSubset[P, S, PHL <: HList, SHL <: HList](implicit
      notEq: P =:!= S,
      pgen: LabelledGeneric.Aux[P, PHL],
      sgen: LabelledGeneric.Aux[S, SHL],
      hlSubset: Lazy[PropSubset[PHL, SHL]]
  ): PropSubset[P, S] =
    anySubset.asInstanceOf[PropSubset[P, S]]
}
