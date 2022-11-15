package ru.tinkoff.tcb.bson

import derevo.derive
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.bson.derivation.bsonDecoder
import ru.tinkoff.tcb.bson.derivation.bsonEncoder

@derive(bsonEncoder, bsonDecoder)
case class TestEntity(_id: String, name: String, externalKey: Option[Int])

class PatchGeneratorSpec extends AnyFunSuite with Matchers {
  test("Generate update with Some") {
    val entity = TestEntity("42", "name", Some(442))

    val (_, patch) = PatchGenerator.mkPatch(entity)

    patch shouldBe BsonDocument(
      "$set" -> BsonDocument(
        "name"        -> "name",
        "externalKey" -> 442
      )
    )
  }

  test("Generate update with None") {
    val entity = TestEntity("42", "name", None)

    val (_, patch) = PatchGenerator.mkPatch(entity)

    patch shouldBe BsonDocument(
      "$set" -> BsonDocument(
        "name" -> "name"
      ),
      "$unset" -> BsonDocument(
        "externalKey" -> ""
      )
    )
  }
}
