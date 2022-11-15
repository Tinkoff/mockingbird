package ru.tinkoff.tcb.utils

package object controlflow {
  @inline def partial[T](f: => PartialFunction[T, T]): T => T = t => f.applyOrElse(t, identity[T])
}
