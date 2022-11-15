package ru.tinkoff.tcb.bson.enumeratum

import org.mongodb.scala.bson.*
import org.scalatest.OptionValues.*
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.bson.BsonDecoder
import ru.tinkoff.tcb.bson.BsonEncoder

class EnumBsonHandlerSpec extends AnyFunSpec with Matchers {
  testScenario(
    descriptor = "normal operation (no transformations)",
    reader = EnumHandler.reader(Dummy),
    expectedReadSuccesses = Map("A" -> Dummy.A, "C" -> Dummy.C),
    expectedReadFails = Seq("c"),
    writer = EnumHandler.writer(Dummy),
    expectedWrites = Map(Dummy.A -> "A", Dummy.C -> "C")
  )

  testScenario(
    descriptor = "case insensitive",
    reader = EnumHandler.reader(`enum` = Dummy, insensitive = true),
    expectedReadSuccesses = Map("A" -> Dummy.A, "a" -> Dummy.A, "C" -> Dummy.C),
    expectedReadFails = Nil,
    writer = EnumHandler.writer(Dummy),
    expectedWrites = Map(Dummy.A -> "A", Dummy.C -> "C")
  )

  testScenario(
    descriptor = "lower case transformed",
    reader = EnumHandler.readerLowercaseOnly(Dummy),
    expectedReadSuccesses = Map("a" -> Dummy.A, "b" -> Dummy.B, "c" -> Dummy.C),
    expectedReadFails = Seq("A", "B", "C"),
    writer = EnumHandler.writerLowercase(Dummy),
    expectedWrites = Map(Dummy.A -> "a", Dummy.C -> "c")
  )

  testScenario(
    descriptor = "upper case transformed",
    reader = EnumHandler.readerUppercaseOnly(Dummy),
    expectedReadSuccesses = Map("A" -> Dummy.A, "B" -> Dummy.B, "C" -> Dummy.C),
    expectedReadFails = Seq("c"),
    writer = EnumHandler.writerUppercase(Dummy),
    expectedWrites = Map(Dummy.A -> "A", Dummy.C -> "C")
  )

  private def testScenario(
      descriptor: String,
      reader: BsonDecoder[Dummy],
      expectedReadSuccesses: Map[String, Dummy],
      expectedReadFails: Seq[String],
      writer: BsonEncoder[Dummy],
      expectedWrites: Map[Dummy, String]
  ): Unit = describe(descriptor) {

    val expectedReadErrors =
      expectedReadFails.map(BsonString(_)) ++ Seq(BsonString("D"), BsonInt32(2))

    def readTests(theReader: BsonDecoder[Dummy]): Unit = {
      it("should work with valid values") {
        expectedReadSuccesses.foreach { case (k, v) =>
          theReader.fromBson(BsonString(k)).toOption.value shouldBe v
        }
      }

      it("should fail with invalid values") {
        expectedReadErrors.foreach(v => theReader.fromBson(v).toOption.isEmpty shouldBe true)
      }
    }

    def writeTests(theWriter: BsonEncoder[Dummy]): Unit =
      it("should write enum values to BSONString") {
        expectedWrites.foreach { case (k, v) =>
          theWriter.toBson(k) shouldBe BsonString(v)
        }
      }

    describe("BSONReader") {
      readTests(reader)
    }

    describe("BSONWriter") {
      writeTests(writer)
    }
  }
}
