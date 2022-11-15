package ru.tinkoff.tcb.bson.derivation

import java.time.Instant
import java.time.Year

import org.mongodb.scala.bson.BsonArray
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.TryValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.bson.BsonEncoder.ops.*
import ru.tinkoff.tcb.bson.{TestEntity => _, _}

class BsonDecoderSpec extends AnyFunSuite with Matchers with TryValues {
  test("decode XXXCaseClass") {
    val doc = BsonDocument(
      "a" -> 1,
      "b" -> 2,
      "c" -> 3,
      "d" -> 4,
      "e" -> 5,
      "f" -> 6,
      "g" -> 7,
      "h" -> 8,
      "i" -> 9,
      "j" -> 10,
      "k" -> 11,
      "l" -> 12,
      "m" -> 13,
      "n" -> 14,
      "o" -> 15,
      "p" -> 16,
      "q" -> 17,
      "r" -> 18,
      "s" -> 19,
      "t" -> 20,
      "u" -> 21,
      "v" -> 22,
      "w" -> 23,
      "x" -> 24,
      "y" -> 25,
      "z" -> 26
    )

    val result = BsonDecoder[XXXCaseClass].fromBson(doc).success.value

    result shouldBe XXXCaseClass(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
      25, 26)
  }

  test("complex entity test") {
    val testDoc = BsonDocument(
      "_id"  -> 42,
      "name" -> "Peka",
      "meta" -> BsonDocument(
        "time" -> Instant.ofEpochSecond(1504787696).bson,
        "seq"  -> 228L,
        "flag" -> false
      ),
      "linkId" -> 721,
      "checks" -> BsonArray(
        BsonDocument(
          "year"    -> Year.of(2018).bson,
          "comment" -> "valid"
        )
      )
    )

    val entity = BsonDecoder[TestEntity].fromBson(testDoc).success.value

    entity shouldEqual TestEntity(
      42,
      "Peka",
      TestMeta(Instant.ofEpochSecond(1504787696), 228, flag = false),
      None,
      Some(721),
      Seq(TestCheck(Year.of(2018), "valid"))
    )
  }

  test("complex entity with defaults test") {
    val testDoc = BsonDocument(
      "_id" -> 42,
      "meta" -> BsonDocument(
        "time" -> Instant.ofEpochSecond(1504787696).bson,
        "seq"  -> 228L,
        "flag" -> false
      ),
      "linkId" -> 721
    )

    val entity = BsonDecoder[TestEntityWithDefaults].fromBson(testDoc).success.value

    entity shouldEqual TestEntityWithDefaults(
      42,
      "test",
      TestMeta(Instant.ofEpochSecond(1504787696), 228, flag = false),
      None,
      Some(721),
      Seq()
    )
  }

  test("container test") {
    val testDoc = BsonDocument(
      "value" -> 42
    )

    val entity = BsonDecoder[TestContainer[Int]].fromBson(testDoc).success.value

    entity shouldEqual TestContainer(Some(42))
  }
}
