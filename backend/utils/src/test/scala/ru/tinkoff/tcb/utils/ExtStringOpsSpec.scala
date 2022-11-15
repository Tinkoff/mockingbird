package ru.tinkoff.tcb.utils

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.utils.string.*

class ExtStringOpsSpec extends AnyFunSuite with Matchers {
  test("camel2Underscore") {
    "peka".camel2Underscore shouldBe "peka"
    "Peka".camel2Underscore shouldBe "peka"
    "pekaYobaPshhh".camel2Underscore shouldBe "peka_yoba_pshhh"
    "PekaYobaPshhh".camel2Underscore shouldBe "peka_yoba_pshhh"
  }

  test("underscore2Camel") {
    "peka".underscore2Camel shouldBe "peka"
    "Peka".underscore2Camel shouldBe "peka"
    "peka_yoba_pshhh".underscore2Camel shouldBe "pekaYobaPshhh"
    "PEKA".underscore2Camel shouldBe "peka"
  }

  test("underscore2UpperCamel") {
    "peka".underscore2UpperCamel shouldBe "Peka"
    "Peka".underscore2UpperCamel shouldBe "Peka"
    "peka_yoba_pshhh".underscore2UpperCamel shouldBe "PekaYobaPshhh"
    "PEKA".underscore2UpperCamel shouldBe "Peka"
  }
}
