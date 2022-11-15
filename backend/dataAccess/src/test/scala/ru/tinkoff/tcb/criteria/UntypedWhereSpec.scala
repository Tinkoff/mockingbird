package ru.tinkoff.tcb.criteria

import org.mongodb.scala.bson.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class UntypedWhereSpec extends AnyFlatSpec with Matchers {
  import Untyped.*

  "An Untyped where" should "support 1 placeholder" in {
    val q = where {
      _.a === 1
    }

    q.toJson shouldBe BsonDocument("a" -> BsonInt32(1)).toJson
  }

  it should "support 2 placeholders" in {
    val q = where {
      _.a === 1 && _.b === 2
    }

    q.toJson shouldBe BsonDocument(
      "$and" -> BsonArray(
        BsonDocument("a" -> BsonInt32(1)),
        BsonDocument("b" -> BsonInt32(2))
      )
    ).toJson
  }

  it should "support 3 placeholders" in {
    val q = where {
      _.a === 1 && _.b === 2 && _.c === 3
    }

    q.toJson shouldBe BsonDocument(
      "$and" -> BsonArray(
        BsonDocument("a" -> BsonInt32(1)),
        BsonDocument("b" -> BsonInt32(2)),
        BsonDocument("c" -> BsonInt32(3))
      )
    ).toJson
  }

  /// The library supports from 1 to 22 placeholders for the where method.
  it should "support 22 placeholders" in {
    val q = where {
      _.p === 0 &&
      _.p === 0 &&
      _.p === 0 &&
      _.p === 0 &&
      _.p === 0 &&
      _.p === 0 &&
      _.p === 0 &&
      _.p === 0 &&
      _.p === 0 &&
      _.p === 0 &&
      _.p === 0 &&
      _.p === 0 &&
      _.p === 0 &&
      _.p === 0 &&
      _.p === 0 &&
      _.p === 0 &&
      _.p === 0 &&
      _.p === 0 &&
      _.p === 0 &&
      _.p === 0 &&
      _.p === 0 &&
      _.p === 0
    }

    q.toJson shouldBe BsonDocument(
      "$and" -> BsonArray.fromIterable(
        List.fill(22)(BsonDocument("p" -> BsonInt32(0)))
      )
    ).toJson
  }
}
