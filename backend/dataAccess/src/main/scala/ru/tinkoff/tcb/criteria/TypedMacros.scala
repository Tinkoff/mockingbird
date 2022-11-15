package ru.tinkoff.tcb.criteria

import scala.annotation.nowarn
import scala.reflect.macros.blackbox.Context

object TypedMacros {
  /// Class Imports
  import Typed.PropertyAccess // scalastyle:ignore

  @nowarn("cat=unused-pat-vars")
  def createTerm[T <: AnyRef: c.WeakTypeTag, U: c.WeakTypeTag](c: Context {
    type PrefixType = PropertyAccess[T]
  })(statement: c.Tree): c.Tree = {
    import c.universe.* // scalastyle:ignore

    val q"""(..$args) => $select""" = statement: @unchecked

    val selectors = select
      .collect { case Select(_, TermName(property)) =>
        property;
      }
      .reverse
      .mkString(".")

    val sourceType   = weakTypeOf[T]
    val propertyType = weakTypeOf[U]

    q"""new Term[${sourceType}, ${propertyType}] (${selectors})"""
  }
}
