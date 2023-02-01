package ru.tinkoff.tcb.criteria

import org.mongodb.scala.bson.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.bson.*

class UntypedCriteriaSpec extends AnyFlatSpec with Matchers {
  import Untyped.*

  "An Untyped criteria" should "support equality comparisons" in {
    (criteria.myField === "a value").toJson shouldBe BsonDocument(
      "myField" -> BsonString("a value")
    ).toJson()

    (criteria.myField === "a value").toJson shouldBe BsonDocument(
      "myField" -> BsonString("a value")
    ).toJson
  }

  it should "support inequality comparisons" in {
    (criteria.myField !== "a value").toJson shouldBe BsonDocument(
      "myField" -> BsonDocument("$ne" -> BsonString("a value"))
    ).toJson

    (criteria.myField =/= "a value").toJson shouldBe BsonDocument(
      "myField" -> BsonDocument("$ne" -> BsonString("a value"))
    ).toJson

    (criteria.myField <> "a value").toJson shouldBe BsonDocument(
      "myField" -> BsonDocument("$ne" -> BsonString("a value"))
    ).toJson
  }

  it should "support multi-value equality" in {
    criteria.ranking.in(1 to 5).toJson shouldBe BsonDocument(
      "ranking" -> BsonDocument(
        "$in" -> BsonArray(
          BsonInt32(1),
          BsonInt32(2),
          BsonInt32(3),
          BsonInt32(4),
          BsonInt32(5)
        )
      )
    ).toJson
  }

  it should "support multi-value inequality" in {
    (!criteria.ranking.in(0, 1, 2)).toJson shouldBe BsonDocument(
      "ranking" -> BsonDocument(
        "$nin" -> BsonArray(
          BsonInt32(0),
          BsonInt32(1),
          BsonInt32(2)
        )
      )
    ).toJson
  }

  it should "support nested object selectors" in {
    val q = criteria.outer.inner =/= 99

    q.toJson shouldBe BsonDocument(
      "outer.inner" -> BsonDocument("$ne" -> BsonInt32(99))
    ).toJson
  }

  it should "support String operations" in {
    val q = criteria.str =~ "^test|re"

    q.toJson shouldBe BsonDocument(
      "str" -> BsonDocument(
        "$regex" -> BsonRegularExpression("^test|re", "")
      )
    ).toJson
  }

  it should "support String operations with flags" in {
    val q = criteria.str =~ "^test|re" -> (MultilineMatching | IgnoreCase)

    q.toJson shouldBe BsonDocument(
      "str" -> BsonDocument(
        "$regex" -> BsonRegularExpression("^test|re", "mi")
      )
    ).toJson
  }

  it should "support conjunctions" in {
    val q = criteria.first < 10 && criteria.second >= 20.0

    BsonDocument(q.element).toJson shouldBe BsonDocument(
      "$and" -> BsonArray(
        BsonDocument(
          "first" -> BsonDocument(
            "$lt" -> BsonInt32(10)
          )
        ),
        BsonDocument(
          "second" -> BsonDocument(
            "$gte" -> BsonDouble(20.0)
          )
        )
      )
    ).toJson
  }

  it should "support disjunctions" in {
    val q = criteria.first < 10 || criteria.second >= 20.0

    BsonDocument(q.element).toJson shouldBe BsonDocument(
      "$or" -> BsonArray(
        BsonDocument(
          "first" -> BsonDocument("$lt" -> BsonInt32(10))
        ),
        BsonDocument(
          "second" -> BsonDocument(
            "$gte" -> BsonDouble(20.0)
          )
        )
      )
    ).toJson
  }

  it should "combine adjacent conjunctions" in {
    val q = criteria.first < 10 &&
      criteria.second >= 20.0 &&
      criteria.third < 0.0

    BsonDocument(q.element).toJson shouldBe BsonDocument(
      "$and" -> BsonArray(
        BsonDocument(
          "first" -> BsonDocument("$lt" -> BsonInt32(10))
        ),
        BsonDocument(
          "second" -> BsonDocument(
            "$gte" -> BsonDouble(20.0)
          )
        ),
        BsonDocument(
          "third" -> BsonDocument("$lt" -> BsonDouble(0.0))
        )
      )
    ).toJson
  }

  it should "combine adjacent disjunctions" in {
    val q = criteria.first < 10 ||
      criteria.second >= 20.0 ||
      criteria.third < 0.0

    BsonDocument(q.element).toJson shouldBe BsonDocument(
      "$or" -> BsonArray(
        BsonDocument(
          "first" -> BsonDocument("$lt" -> BsonInt32(10))
        ),
        BsonDocument(
          "second" -> BsonDocument(
            "$gte" -> BsonDouble(20.0)
          )
        ),
        BsonDocument(
          "third" -> BsonDocument("$lt" -> BsonDouble(0.0))
        )
      )
    ).toJson
  }

  it should "support compound filtering" in {
    val q = criteria.first < 10 &&
      (criteria.second >= 20.0 || criteria.second.in(0.0, 1.0))

    q.toJson shouldBe BsonDocument(
      "$and" -> BsonArray(
        BsonDocument(
          "first" -> BsonDocument("$lt" -> BsonInt32(10))
        ),
        BsonDocument(
          "$or" -> BsonArray(
            BsonDocument(
              "second" -> BsonDocument(
                "$gte" -> BsonDouble(20.0)
              )
            ),
            BsonDocument(
              "second" -> BsonDocument(
                "$in" -> BsonArray(
                  BsonDouble(0.0),
                  BsonDouble(1.0)
                )
              )
            )
          )
        )
      )
    ).toJson
  }

  it should "support alternating logical operators" in {
    val q = criteria.first < 10 &&
      criteria.second >= 20.0 ||
      criteria.third < 0.0 &&
      criteria.fourth =~ "some regex"

    BsonDocument(q.element).toJson shouldBe BsonDocument(
      "$or" -> BsonArray(
        BsonDocument(
          "$and" -> BsonArray(
            BsonDocument(
              "first" -> BsonDocument(
                "$lt" -> BsonInt32(10)
              )
            ),
            BsonDocument(
              "second" -> BsonDocument(
                "$gte" -> BsonDouble(20.0)
              )
            )
          )
        ),
        BsonDocument(
          "$and" -> BsonArray(
            BsonDocument(
              "third" -> BsonDocument(
                "$lt" -> BsonDouble(0.0)
              )
            ),
            BsonDocument(
              "fourth" -> BsonDocument(
                "$regex" -> BsonRegularExpression("some regex", "")
              )
            )
          )
        )
      )
    ).toJson
  }

  it should "support logical negation" in {
    (!(criteria.a === 42)).toJson shouldBe BsonDocument(
      "a" -> BsonDocument("$ne" -> BsonInt32(42))
    ).toJson

    (!(criteria.a =~ "regex(p)?")).toJson shouldBe BsonDocument(
      "$not" -> BsonDocument(
        "a" -> BsonDocument(
          "$regex" -> BsonRegularExpression("regex(p)?", "")
        )
      )
    ).toJson

    (
      !(criteria.xyz === 1 || criteria.xyz === 2)
    ).toJson shouldBe BsonDocument(
      "$nor" -> BsonArray(
        BsonDocument("xyz" -> BsonInt32(1)),
        BsonDocument("xyz" -> BsonInt32(2))
      )
    ).toJson
  }

  it should "have an 'empty' resulting in no criteria" in {
    Expression.empty.toJson shouldBe BsonDocument().toJson
  }

  it should "ignore 'empty' in logical operators" in {
    (criteria.a === 1 && Expression.empty).toJson shouldBe BsonDocument("a" -> BsonInt32(1)).toJson

    (Expression.empty && criteria.a === 2.0).toJson shouldBe BsonDocument("a" -> BsonDouble(2.0)).toJson

    (
      Expression.empty || criteria.a === "three"
    ).toJson shouldBe BsonDocument("a" -> BsonString("three")).toJson

    (criteria.a === 4L || Expression.empty).toJson shouldBe BsonDocument("a" -> BsonInt64(4L)).toJson
  }

  it should "support negative existence constraints" in {
    (!criteria.a.exists).toJson shouldBe BsonDocument(
      "a" -> BsonDocument("$exists" -> BsonBoolean(false))
    ).toJson
  }

  it should "correctly negate and" in {
    (
      !(criteria.name === "peka" && criteria.id === 42) &&
        !(criteria.list in ("a", "b", "c"))
    ).toJson shouldBe BsonDocument(
      "$and" -> BsonArray(
        BsonDocument(
          "$not" -> BsonDocument(
            "$and" -> BsonArray(
              BsonDocument(
                "name" -> BsonString("peka")
              ),
              BsonDocument(
                "id" -> BsonInt32(42)
              )
            )
          )
        ),
        BsonDocument(
          "list" -> BsonDocument(
            "$nin" -> BsonArray(
              "a",
              "b",
              "c"
            )
          )
        )
      )
    ).toJson
  }
}
