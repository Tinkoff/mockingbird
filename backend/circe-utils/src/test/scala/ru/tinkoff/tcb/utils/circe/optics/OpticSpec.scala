package ru.tinkoff.tcb.utils.circe.optics

import io.circe.*
import io.circe.literal.*
import io.circe.syntax.*
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.refspec.RefSpec

class OpticSpec extends RefSpec with Matchers {
  object `Optic instance` {
    val optic = JLens \ "outer" \ "inner"

    object `value setter` {
      val setter = optic.set(42.asJson)

      def `should create fields recursively in empty JSON`: Assertion = {
        val target = Json.obj()
        val result = setter(target)

        result shouldBe json"""{"outer": {"inner": 42}}"""
      }

      def `should replace existing value`: Assertion = {
        val target = json"""{"outer": {"inner": 12}}"""
        val result = setter(target)

        result shouldBe json"""{"outer": {"inner": 42}}"""
      }

      def `should keep target contents`: Assertion = {
        val target = json"""{"a": {"b": "c"}}"""
        val result = setter(target)

        result shouldBe json"""{"outer": {"inner": 42}, "a": {"b": "c"}}"""
      }
    }

    object `optional setter` {
      def `should update if arg is Some(..)` : Assertion = {
        val setter = optic.setOpt(42.asJson.some)
        val target = json"""{"outer": {"inner": 12}}"""
        val result = setter(target)

        result shouldBe json"""{"outer": {"inner": 42}}"""
      }

      def `should prune if arg is None`: Assertion = {
        val setter = optic.setOpt(None)
        val target = json"""{"outer": {"inner": 42}, "a": {"b": "c"}}"""
        val result = setter(target)

        result shouldBe json"""{"outer": {},"a": {"b": "c"}}"""
      }
    }

    object `array setter` {
      val setter = optic \ 2

      def `should keep array contents`: Assertion = {
        val target = json"""{"outer": {"inner": [1, 2, 3]}}"""
        val result = setter.set(json"4")(target)

        result shouldBe json"""{"outer": {"inner": [1, 2, 4]}}"""
      }

      def `should set fields inside arrays`: Assertion = {
        val target  = json"""{"outer": {"inner": [{"v": 1}, {"v": 2}, {"v": 3}]}}"""
        val setter2 = setter \ "v"
        val result  = setter2.set(json"4")(target)

        result shouldBe json"""{"outer": {"inner": [{"v": 1}, {"v": 2}, {"v": 4}]}}"""
      }

      def `should write at correct index`: Assertion = {
        val opt = JLens \ "outer" \ "inner" \ 1

        opt.set(100.asJson)(json"""{"outer": {"inner": [42]}}""") shouldBe json"""{"outer": {"inner": [42, 100]}}"""
      }

      def `should append fields inside arrays`: Assertion = {
        val target  = json"""{"inner": [{"v": 1}, {"v": 2}, {"v": 3}]}"""
        val setter2 = JLens \ "inner" \ 0 \ "vv"
        val result  = setter2.set(json"4")(target)

        result shouldBe json"""{"inner": [{"v": 1, "vv": 4}, {"v": 2}, {"v": 3}]}"""

        val setter3 = JLens \ "inner" \ 1 \ "vv"
        val result2 = setter3.set(json"4")(target)

        result2 shouldBe json"""{"inner": [{"v": 1}, {"v": 2, "vv": 4}, {"v": 3}]}"""
      }
    }

    object `json getter` {
      val jgetter = optic.get

      def `should extract json`: Assertion = {
        val target = json"""{"outer": {"inner": {"a": {"b": "c"}}}}"""
        val result = jgetter(target)

        result shouldBe json"""{"a": {"b": "c"}}"""
      }

      def `should return empty json if there is no subtree`: Assertion = {
        val target = json"""{"a": {"b": "c"}}"""
        val result = jgetter(target)

        result shouldBe Json.Null
      }
    }

    object `json prune` {
      val prune = optic.prune

      def `should do nothing if there is no subtree`: Assertion = {
        val target = json"""{"a": {"b": "c"}}"""
        val result = prune(target)

        result shouldBe target
      }

      def `should cut only redundant part of subtree`: Assertion = {
        val target = json"""{"outer": {"inner": 42, "other": {"b": "c"}}}"""
        val result = prune(target)

        result shouldBe json"""{"outer": {"other": {"b": "c"}}}"""
      }
    }

    object `json validate` {
      val isOk = optic.validate

      def `should return true if subtree exists`: Assertion = {
        val target = json"""{"outer": {"inner": 42}}"""

        isOk(target) shouldBe true
      }

      def `should return false if there is no valid subtree`: Assertion = {
        val target = json"""{"outer": {"other": {"b": "c"}}}"""

        isOk(target) shouldBe false
      }
    }

    object `json modify` {
      def `should modify as expected`: Assertion = {
        val target = json"""{"value": 2}"""

        val optic = JLens \ "value"

        optic.modify(_.withNumber(jn => Json.fromInt(jn.toInt.get * 2)))(target) shouldBe
          json"""{"value": 4}"""
      }

      def `should modify array as expected`: Assertion = {
        val target = json"""{"value": [1, 2, 3]}"""

        val optic = (JLens \ "value").traverse

        optic.modify(_.withNumber(jn => Json.fromInt(jn.toInt.get * 2)))(target) shouldBe
          json"""{"value": [2, 4, 6]}"""
      }
    }

    object `json modifyOpt` {
      def `should modify as expected`: Assertion = {
        val target = json"""{"value": 2}"""

        val optic = JLens \ "value"

        optic.modifyOpt {
          case Some(json) => json.withNumber(jn => Json.fromInt(jn.toInt.get * 2))
          case None       => Json.fromInt(2)
        }(target) shouldBe
          json"""{"value": 4}"""
      }

      def `should set if there is no value`: Assertion = {
        val target = json"""{}"""

        val optic = JLens \ "value"

        optic.modifyOpt {
          case Some(json) => json.withNumber(jn => Json.fromInt(jn.toInt.get * 2))
          case None       => Json.fromInt(2)
        }(target) shouldBe
          json"""{"value": 2}"""
      }
    }

    object `json modifyObjectValues` {
      def `should modify as expected`: Assertion = {
        val target = json"""{"outer": {"inner": 42}}"""

        val optic = JLens \ "outer"

        optic.modifyObjectValues(_.withNumber(jn => Json.fromInt(jn.toInt.get * 2)))(target) shouldBe
          json"""{"outer": {"inner": 84}}"""
      }
    }

    object `json modifyFields` {
      def `should modify as expected`: Assertion = {
        val target = json"""{"outer": {"inner": 42}}"""

        val optic = JLens \ "outer"

        optic.modifyFields { case (key, value) =>
          key -> value.withNumber(jn => Json.fromInt(jn.toInt.get * 2))
        }(target) shouldBe json"""{"outer": {"inner": 84}}"""
      }
    }

    object `optic composition` {
      def `should produce working optic`: Assertion = {
        val op1   = JsonOptic.forPath("outer")
        val op2   = JsonOptic.forPath("inner")
        val optic = op1 \\ op2

        val result = optic.get(json"""{"outer": {"inner": 42}}""")
        result shouldBe json"42"
      }
    }
  }

  object `JsonOptic companion` {
    object `fromPathString method` {
      def `should construct JsonOptic`: Assertion = {
        val result = JsonOptic.fromPathString("a.[1].b")

        result.path shouldBe "a.[1].b"
      }
    }
  }

  object `Optic with traverse` {
    val optic = JsonOptic.fromPathString("a.$.b")

    object `value setter` {
      def `should replace existing values`: Assertion = {
        val target = json"""{"a": [{"b": 1}, {"b": 2}, {"b": 3}]}"""
        val result = optic.set(Json.fromInt(4))(target)

        result shouldBe json"""{"a": [{"b": 4}, {"b": 4}, {"b": 4}]}"""
      }

      def `should properly re-create complete structure`: Assertion = {
        val target = Json.obj()
        val result = optic.set(Json.fromInt(4))(target)

        result shouldBe json"""{"a": [{"b": 4}]}"""
      }
    }

    object `json getter` {
      def `should get subfields from array`: Assertion = {
        val target = json"""{"a": [{"b": 1}, {"b": 2}, {"b": 3}]}"""
        val result = optic.get(target)

        result shouldBe json"""[1, 2, 3]"""
      }
    }
  }

  object `Optic with only traverse` {
    val optic = JLens.traverse

    object `json validate` {
      def `should succeed on array`: Assertion = {
        val target = json"""[]"""

        optic.validate(target) shouldBe true
      }

      def `should fail on object`: Assertion = {
        val target = json"""{}"""

        optic.validate(target) shouldBe false
      }

      def `should fail on primitive`: Assertion = {
        val target = json"""42"""

        optic.validate(target) shouldBe false
      }
    }
  }
}
