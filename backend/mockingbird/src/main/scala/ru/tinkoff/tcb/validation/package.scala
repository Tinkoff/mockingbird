package ru.tinkoff.tcb

package object validation {
  type Rule[-T] = T => Vector[String]
}
