package ru.tinkoff.tcb.utils.circe

import io.circe.literal.*
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.refspec.RefSpec

class JsonOpsTest extends RefSpec with Matchers {
  object `A Json instance` {
    object `called with camelizeKeys` {
      def `should just work`: Assertion = {
        val doc = json"""
        {
          "first_name" : "foo",
          "last_name" : "bar",
          "parent" : {
            "first_name" : "baz",
            "last_name" : "bazz"
          }
        }
        """

        val res = doc.camelizeKeys

        res shouldBe json"""{
        "firstName" : "foo",
        "lastName" : "bar",
        "parent" : {
          "firstName" : "baz",
          "lastName" : "bazz"
        }
      }"""
      }
    }
  }
}
