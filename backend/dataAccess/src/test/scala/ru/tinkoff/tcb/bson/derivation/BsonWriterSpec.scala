package ru.tinkoff.tcb.bson.derivation

import java.time.Instant
import java.time.Year
import scala.annotation.nowarn
import scala.jdk.CollectionConverters.*

import org.mongodb.scala.bson.*
import org.scalactic.Equality
import org.scalactic.Prettifier
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.bson.BsonEncoder.ops.*
import ru.tinkoff.tcb.bson.{TestEntity as _, *}

class BsonWriterSpec extends AnyFunSuite with Matchers {
  @nowarn("cat=unused-privates")
  implicit private val bdocEq: Equality[BsonDocument] = (a: BsonDocument, b: Any) =>
    b match {
      // noinspection SameElementsToEquals
      case BDocument(d2) =>
        a.asScala.toVector sameElements d2.toVector
      case _ => false
    }

  implicit private val bdocPretty: Prettifier =
    Prettifier { case doc: BsonDocument => doc.toJson }

  test("encode XXXCaseClass") {
    val instance =
      XXXCaseClass(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26)

    val result = BsonEncoder[XXXCaseClass].toBson(instance)

    result shouldEqual BsonDocument(
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
  }

  test("complex entity test") {
    val testData = TestEntity(
      42,
      "Peka",
      TestMeta(Instant.ofEpochSecond(1504787696), 228, flag = false),
      None,
      Some(721),
      Seq(TestCheck(Year.of(2018), "valid"))
    )

    val doc = BsonEncoder[TestEntity].toBson(testData)

    doc shouldEqual BsonDocument(
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
  }

  test("container test") {
    val testData = TestContainer(Some(42))

    val doc = BsonEncoder[TestContainer[Int]].toBson(testData)

    doc shouldEqual BsonDocument(
      "value" -> 42
    )
  }
}
