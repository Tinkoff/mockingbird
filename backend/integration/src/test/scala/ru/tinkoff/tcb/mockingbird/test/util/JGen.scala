package ru.tinkoff.tcb.mockingbird.test.util

import io.circe.Json
import org.scalacheck.Arbitrary
import org.scalacheck.Gen

object JGen {
  val jNull: Gen[Json] = Gen.const(Json.Null)

  val jString: Gen[Json] =
    Gen.alphaNumStr.map(_.take(15)).map(Json.fromString)

  val jLong: Gen[Json] =
    Arbitrary.arbitrary[Long].map(Json.fromLong)

  val jDouble: Gen[Json] =
    Arbitrary.arbitrary[Double].map(Json.fromDouble).withFilter(_.nonEmpty).map(_.get)

  val jNumber: Gen[Json] = Gen.oneOf(jLong, jDouble)

  val jBoolean: Gen[Json] = Arbitrary.arbitrary[Boolean].map(Json.fromBoolean)

  def sJArray(size: Int): Gen[Json] = Gen.containerOfN[Seq, Json](size, sJson(size / 2)).map(Json.fromValues)

  def jField(size: Int): Gen[(String, Json)] =
    for {
      name <- Gen.alphaLowerStr
      if name.nonEmpty
      value <- sJson(size)
    } yield name.take(10) -> value

  def sJObject(size: Int): Gen[Json] =
    Gen.containerOfN[Seq, (String, Json)](size, jField(size / 2)).map(Json.fromFields)

  def sJson(size: Int): Gen[Json] =
    if (size <= 0) {
      Gen.oneOf(jNull, jString, jNumber, jBoolean)
    } else {
      Gen.oneOf(jNull, jString, jNumber, jBoolean, sJArray(size - 1), sJObject(size - 1))
    }

  val jArray: Gen[Json] = sJArray(7)

  val jObject: Gen[Json] = sJObject(7)

  val json: Gen[Json] = sJson(7)
}
