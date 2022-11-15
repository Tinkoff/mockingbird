package ru.tinkoff.tcb.criteria

import org.mongodb.scala.bson.BsonInt32
import org.mongodb.scala.bson.BsonString
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.bson.*
import ru.tinkoff.tcb.bson.derivation.*
import ru.tinkoff.tcb.criteria.Typed.*

case class Data(intField: Int, optField: Option[Int])

object Data {
  implicit val dataDecoder: BsonDecoder[Data] = DerivedDecoder.genBsonDecoder[Data]
  implicit val dataEncoder: BsonEncoder[Data] = DerivedEncoder.genBsonEncoder[Data]
}

class UpdateExpressionSpec extends AnyFunSuite with Matchers {
  test("plain field should be set as-is") {
    prop[Data](_.intField).set(42) shouldBe UpdateExpression("$set", "intField" -> BsonInt32(42))
  }

  test("option field should be set as-is") {
    prop[Data](_.optField)
      .setOp(Option(42)) shouldBe UpdateExpression("$set", "optField" -> BsonInt32(42))
  }

  test("option field should be unset on None") {
    prop[Data](_.optField)
      .setOp(Option.empty[Int]) shouldBe UpdateExpression("$unset", "optField" -> BsonString(""))
  }
}
