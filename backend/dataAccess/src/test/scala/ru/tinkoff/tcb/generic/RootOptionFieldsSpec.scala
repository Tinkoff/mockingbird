package ru.tinkoff.tcb.generic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class RootOptionFieldsSpec extends AnyFunSuite with Matchers {
  case class Entity(
      id: Int,
      name: String,
      data: Option[Vector[String]],
      description: Option[String]
  )

  test("Fields are correct") {
    RootOptionFields[Entity].fields shouldBe Set("data", "description")
  }
}
