package ru.tinkoff.tcb.generic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class FieldsSpec extends AnyFunSuite with Matchers {
  case class Evidence()

  test("Fields of empty case class") {
    Fields[Evidence].fields shouldBe Nil
  }

  case class Projection(ev: Option[Evidence], label: String)

  test("Fields of Projection") {
    Fields[Projection].fields shouldBe List("ev", "label")
  }

  sealed trait ST
  case class A(a: Int) extends ST
  case class B(b: Int) extends ST

  test("Fields of sealed trait") {
    Fields[ST].fields shouldBe Nil
  }
}
