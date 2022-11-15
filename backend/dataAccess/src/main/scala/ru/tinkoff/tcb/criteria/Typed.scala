package ru.tinkoff.tcb.criteria

object Typed {

  /// Class Types
  /**
   * The '''PropertyAccess''' type exists for syntactic convenience when the `criteria` method is used. The tandem allow
   * for constructs such as:
   *
   * {{{
   * import Typed.*
   *
   * val typeCheckedQuery = criteria[SomeType] (_.first) < 10 && (
   *     criteria[SomeType] (_.second) >= 20.0 ||
   *     criteria[SomeType] (_.second).in (0.0, 1.0)
   *     );
   * }}}
   */
  final class PropertyAccess[ParentT <: AnyRef] {
    def apply[T](statement: ParentT => T): Term[ParentT, T] =
      macro TypedMacros.createTerm[ParentT, T]
  }

  /**
   * The prop method produces a type which enforces the existence of property names within ''T''.
   */
  def prop[T <: AnyRef]: PropertyAccess[T] = new PropertyAccess[T]
}
