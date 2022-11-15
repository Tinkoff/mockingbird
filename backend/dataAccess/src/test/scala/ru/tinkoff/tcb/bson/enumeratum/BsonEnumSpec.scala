package ru.tinkoff.tcb.bson.enumeratum

import org.mongodb.scala.bson.*
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.bson.*

class BsonEnumSpec extends AnyFunSpec with Matchers {
  describe("BSON serdes") {

    describe("deserialisation") {

      it("should work with valid values") {
        val bsonValue: BsonValue = BsonString("A")
        bsonValue.decodeOpt[Dummy].get shouldBe Dummy.A
      }

      it("should fail with invalid values") {
        val strBsonValue: BsonValue = BsonString("D")
        val intBsonValue: BsonValue = BsonInt32(2)

        strBsonValue.decodeOpt[Dummy] shouldBe None
        intBsonValue.decodeOpt[Dummy] shouldBe None
      }
    }
  }
}
