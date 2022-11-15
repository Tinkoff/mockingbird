package ru.tinkoff.tcb.bson.derivation

import derevo.derive
import org.mongodb.scala.bson.BsonDocument
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalatest.TryValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.Checkers

import ru.tinkoff.tcb.bson.*
import ru.tinkoff.tcb.bson.annotation.BsonDiscriminator

class DerivedCodecsSuite extends AnyFunSuite with Checkers with Matchers with TryValues {

  // Stolen from http://github.com/travisbrown/circe
  @derive(bsonDecoder, bsonEncoder) @BsonDiscriminator("case", _.reverse)
  sealed trait RecursiveAdtExample
  case class BaseAdtExample(a: String) extends RecursiveAdtExample
  case class NestedAdtExample(r: RecursiveAdtExample) extends RecursiveAdtExample
  object RecursiveAdtExample

  private def atDepth(depth: Int): Gen[RecursiveAdtExample] =
    if (depth < 3)
      Gen.oneOf(
        Arbitrary.arbitrary[String].map(BaseAdtExample),
        atDepth(depth + 1).map(NestedAdtExample)
      )
    else Arbitrary.arbitrary[String].map(BaseAdtExample)

  implicit val arbitraryRecursiveAdtExample: Arbitrary[RecursiveAdtExample] =
    Arbitrary(atDepth(0))

  test("identity") {
    check((v: RecursiveAdtExample) =>
      BsonDecoder[RecursiveAdtExample]
        .fromBson(BsonEncoder[RecursiveAdtExample].toBson(v))
        .get == v
    )
  }

  test("decoding failure raises a meaningful error message") {
    BsonDecoder[RecursiveAdtExample]
      .fromBson(BsonDocument())
      .failure
      .exception
      .getMessage shouldBe "No discriminator field (case) found while decoding RecursiveAdtExample"
  }
}
