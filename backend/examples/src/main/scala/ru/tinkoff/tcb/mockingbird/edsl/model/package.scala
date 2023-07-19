package ru.tinkoff.tcb.mockingbird.edsl

import cats.free.Free

package object model {
  type Example[T] = Free[Step, T]
}
