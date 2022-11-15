package ru.tinkoff.tcb.criteria

import scala.language.dynamics

sealed trait Untyped extends Dynamic {
  def selectDynamic(field: String): Term[Any, Any] = Term[Any, Any](field)
}

object Untyped {

  /**
   * The criteria property is a ''factory'' of '''Untyped''' instances.
   */
  val criteria = new Untyped {}

  def where(block: (Untyped) => Expression[Any]): Expression[Any] = block(criteria)

  def where(block: (Untyped, Untyped) => Expression[Any]): Expression[Any] =
    block(criteria, criteria)

  def where(block: (Untyped, Untyped, Untyped) => Expression[Any]): Expression[Any] =
    block(criteria, criteria, criteria)

  def where(block: (Untyped, Untyped, Untyped, Untyped) => Expression[Any]): Expression[Any] =
    block(criteria, criteria, criteria, criteria)

  def where(
      block: (Untyped, Untyped, Untyped, Untyped, Untyped) => Expression[Any]
  ): Expression[Any] =
    block(criteria, criteria, criteria, criteria, criteria)

  def where(
      block: (Untyped, Untyped, Untyped, Untyped, Untyped, Untyped) => Expression[Any]
  ): Expression[Any] =
    block(criteria, criteria, criteria, criteria, criteria, criteria)

  def where(
      block: (Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped) => Expression[Any]
  ): Expression[Any] =
    block(criteria, criteria, criteria, criteria, criteria, criteria, criteria)

  def where(
      block: (Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped, Untyped) => Expression[
        Any
      ]
  ): Expression[Any] =
    block(criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria)

  def where(
      block: (
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped
      ) => Expression[Any]
  ): Expression[Any] =
    block(criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria, criteria)

  def where(
      block: (
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped
      ) => Expression[Any]
  ): Expression[Any] =
    block(
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria
    )

  def where(
      block: (
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped
      ) => Expression[Any]
  ): Expression[Any] =
    block(
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria
    )

  def where(
      block: (
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped
      ) => Expression[Any]
  ): Expression[Any] =
    block(
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria
    )

  def where(
      block: (
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped
      ) => Expression[Any]
  ): Expression[Any] =
    block(
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria
    )

  def where(
      block: (
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped
      ) => Expression[Any]
  ): Expression[Any] =
    block(
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria
    )

  def where(
      block: (
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped
      ) => Expression[Any]
  ): Expression[Any] =
    block(
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria
    )

  def where(
      block: (
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped
      ) => Expression[Any]
  ): Expression[Any] =
    block(
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria
    )

  def where(
      block: (
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped
      ) => Expression[Any]
  ): Expression[Any] =
    block(
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria
    )

  def where(
      block: (
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped
      ) => Expression[Any]
  ): Expression[Any] =
    block(
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria
    )

  def where(
      block: (
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped
      ) => Expression[Any]
  ): Expression[Any] =
    block(
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria
    )

  def where(
      block: (
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped
      ) => Expression[Any]
  ): Expression[Any] =
    block(
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria
    )

  def where(
      block: (
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped
      ) => Expression[Any]
  ): Expression[Any] =
    block(
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria
    )

  def where(
      block: (
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped,
          Untyped
      ) => Expression[Any]
  ): Expression[Any] =
    block(
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria,
      criteria
    )
}
