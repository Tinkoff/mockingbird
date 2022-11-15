package ru.tinkoff.tcb.bson

import scala.util.matching.Regex

import org.scalactic.Equality
import org.scalatest.TryValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.bson.BsonEncoder.ops.*

class RoundRobinSpec extends AnyFunSuite with Matchers with TryValues {
  implicit private val regexEquality: Equality[Regex] =
    (a: Regex, b: Any) =>
      b match {
        case rb: Regex => a.regex == rb.regex
        case _         => false
      }

  test("Regex serialization") {
    val group = "<(?<name>[a-zA-Z0-9]+)>".r

    val sut = BsonDecoder[Regex].fromBson(group.bson)

    sut.success.value shouldEqual group
  }
}
