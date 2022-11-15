package ru.tinkoff.tcb.bson.optics

import scala.jdk.CollectionConverters.*

import org.mongodb.scala.bson.*
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.refspec.RefSpec

class BsonOpticSpec extends RefSpec with Matchers {
  object `BsonOptic instance` {
    val optic = BLens \ "outer" \ "inner"

    object `value setter` {
      val setter = optic.set(BsonInt32(100))

      def `should create fields recursively in empty BsonDocument`: Assertion = {
        val target = BsonDocument()
        val result = setter(target)

        result shouldBe BsonDocument("outer" -> BsonDocument("inner" -> BsonInt32(100)))
      }

      def `should replace existing value`: Assertion = {
        val target = BsonDocument("outer" -> BsonDocument("inner" -> BsonInt32(42)))
        val result = setter(target)

        result shouldBe BsonDocument("outer" -> BsonDocument("inner" -> BsonInt32(100)))
      }

      def `should keep target contents`: Assertion = {
        val target = BsonDocument("a" -> BsonDocument("b" -> "c"))
        val result = setter(target)

        result.asInstanceOf[BsonDocument].asScala should contain theSameElementsAs BsonDocument(
          "a"     -> BsonDocument("b" -> "c"),
          "outer" -> BsonDocument("inner" -> BsonInt32(100))
        ).asScala
      }
    }

    object `BsonArray setter` {
      val setter = optic \ 2

      def `should keep BsonArray contents`: Assertion = {
        val target = BsonDocument("outer" -> BsonDocument("inner" -> BsonArray(1, 2, 3)))
        val result = setter.set(BsonInt32(4))(target)

        result shouldBe BsonDocument("outer" -> BsonDocument("inner" -> BsonArray(1, 2, 4)))
      }

      def `should set fields inside BsonArrays`: Assertion = {
        val target = BsonDocument(
          "outer" -> BsonDocument(
            "inner" -> BsonArray(
              BsonDocument("v" -> 1),
              BsonDocument("v" -> 2),
              BsonDocument("v" -> 3)
            )
          )
        )
        val setter2 = setter \ "v"
        val result  = setter2.set(BsonInt32(4))(target)

        result shouldBe BsonDocument(
          "outer" -> BsonDocument(
            "inner" -> BsonArray(
              BsonDocument("v" -> 1),
              BsonDocument("v" -> 2),
              BsonDocument("v" -> 4)
            )
          )
        )
      }

      def `should write at correct index`: Assertion = {
        val opt = BLens \ "outer" \ "inner" \ 1

        val result =
          opt.set(BsonInt32(100))(BsonDocument("outer" -> BsonDocument("inner" -> BsonArray(42))))

        result.asInstanceOf[BsonDocument].asScala should contain theSameElementsAs BsonDocument(
          "outer" -> BsonDocument("inner" -> BsonArray(42, 100))
        ).asScala
      }
    }

    object `bson getter` {
      val getter = optic.get

      val data = BsonDocument("a" -> BsonDocument("b" -> "c"))

      def `should extract values`: Assertion = {
        val target = BsonDocument("outer" -> BsonDocument("inner" -> data))

        val result = getter(target)

        result shouldBe data
      }

      def `should return BsonNull if there is no subtree`: Assertion = {
        val result = getter(data)

        result shouldBe BsonNull()
      }

      def `should return BsonNull if target is Bson primitive`: Assertion = {
        val result = getter(BsonString("null"))

        result shouldBe BsonNull()
      }
    }

    object `bson validate` {
      val validator = optic.validate

      def `should return true if path exists`: Assertion = {
        val result = validator(BsonDocument("outer" -> BsonDocument("inner" -> 42)))

        result shouldBe true
      }

      def `should return false if path does not exist`: Assertion = {
        val result = validator(BsonDocument("a" -> BsonDocument("b" -> "c")))

        result shouldBe false
      }
    }
  }
}
