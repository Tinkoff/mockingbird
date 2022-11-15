package ru.tinkoff.tcb.instances

import io.circe.JsonNumber

object jsonNumber {
  implicit val jsonNumberOrdering: scala.math.Ordering[JsonNumber] =
    (lhs: JsonNumber, rhs: JsonNumber) => lhs.toBigDecimal.get.compare(rhs.toBigDecimal.get)
}
