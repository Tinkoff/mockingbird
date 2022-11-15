package ru.tinkoff.tcb.utils.instances.predicate

object or {
  implicit def logicalOrMonoid[T]: Monoid[T => Boolean] =
    new Monoid[T => Boolean] {
      override def empty: T => Boolean = _ => false

      override def combine(x: T => Boolean, y: T => Boolean): T => Boolean =
        t => x(t) || y(t)
    }
}
