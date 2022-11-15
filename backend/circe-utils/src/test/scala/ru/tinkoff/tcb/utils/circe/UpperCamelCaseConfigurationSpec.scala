package ru.tinkoff.tcb.utils.circe

import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.refspec.RefSpec

class UpperCamelCaseConfigurationSpec extends RefSpec with Matchers {
  object `UpperCamelCaseConfiguration transformMemberNames` {
    val sut: String => String = UpperCamelCaseConfiguration.circeConfig.transformMemberNames

    def `snake_case test`: Assertion = {
      sut("snake_case") shouldBe "SnakeCase"
      sut("eCredit_id") shouldBe "ECreditId"
    }

    def `lowerCamelCase test`: Assertion =
      sut("lowerCamelCase") shouldBe "LowerCamelCase"
  }
}
