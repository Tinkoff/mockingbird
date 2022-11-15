package ru.tinkoff.tcb.utils.circe

import io.circe.literal.*
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.refspec.RefSpec

class MergerSpec extends RefSpec with Matchers {
  object `smart merge` {
    object `of arrays` {
      def `should shrink long arrays`: Assertion = {
        val j1 = json"""{"a": [0, 1, 2]}"""
        val j2 = json"""{"a": [0, 3]}"""

        j1 +: j2 shouldBe j2
      }

      def `should update and expand`: Assertion = {
        val j1 = json"""{"a": [0, 1, 2]}"""
        val j2 = json"""{"a": [0, 1, 4, 7]}"""

        j1 +: j2 shouldBe j2
      }
    }

    object `of objects` {
      def `should replace sub-object fields`: Assertion = {
        val j1 = json"""{"a": {"c": 1, "d": 2, "e": 3}, "b": 1}"""
        val j2 = json"""{"a": {"c": 11, "d": 22}}"""

        j1 +: j2 shouldBe json"""{"a": {"c": 11, "d": 22, "e": 3}, "b": 1}"""
      }
    }
  }
}
