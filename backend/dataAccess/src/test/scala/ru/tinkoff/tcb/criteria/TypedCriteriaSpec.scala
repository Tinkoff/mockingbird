package ru.tinkoff.tcb.criteria

import derevo.derive
import org.mongodb.scala.bson.*
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.bson.*
import ru.tinkoff.tcb.bson.derivation.*

class TypedCriteriaSpec extends AnyFlatSpec with Matchers {

  /// Class Types
  @derive(bsonEncoder, bsonDecoder)
  case class Grandchild(saying: String)

  @derive(bsonEncoder, bsonDecoder)
  case class Nested(description: String, score: Double, grandchild: Grandchild)

  @derive(bsonEncoder, bsonDecoder)
  case class ExampleDocument(
      age: Int,
      name: String,
      nested: Nested,
      nestedCollection: Seq[Nested],
      grandchildCollection: Seq[Grandchild]
  )

  /// Class Imports
  import Typed.*

  "A Typed criteria" should "support equality comparisons" in {
    /// Since a Typed.criteria is being used, the compiler will enforce
    /// the leaf property types given to the criteria method.
    (prop[ExampleDocument](_.name) =/= "a value").toJson shouldBe
      BsonDocument("name" -> BsonDocument("$ne" -> BsonString("a value"))).toJson

    (prop[ExampleDocument](_.age) === 99).toJson shouldBe
      BsonDocument("age" -> BsonInt32(99)).toJson
  }

  it should "support nested object selectors" in {
    /// Since a Typed.criteria is being used, the compiler will enforce
    /// both the validity of the selector path as well as the ultimate
    /// type referenced (a String in this case).
    val q = prop[ExampleDocument](_.nested.grandchild.saying) =/=
      "something"

    q.toJson shouldBe
      BsonDocument(
        "nested.grandchild.saying" ->
          BsonDocument("$ne" -> "something")
      ).toJson
  }

  it should "support ordering comparisons" in {
    /// Since a Typed.criteria is being used, the compiler will enforce
    /// the leaf property types given to the criteria method.
    (prop[ExampleDocument](_.nested.score) >= 2.3).toJson shouldBe
      BsonDocument(
        "nested.score" ->
          BsonDocument("$gte" -> BsonDouble(2.3))
      ).toJson

    (prop[ExampleDocument](_.age) < 99).toJson shouldBe
      BsonDocument(
        "age" ->
          BsonDocument("$lt" -> BsonInt32(99))
      ).toJson
  }

  it should "support String operations" in {
    val q = prop[ExampleDocument](_.name) =~ "^test|re"

    q.toJson shouldBe
      BsonDocument("name" -> BsonDocument("$regex" -> BsonRegularExpression("^test|re", ""))).toJson
  }

  it should "support String operations with flags" in {
    val q = prop[ExampleDocument](_.name) =~ "^test|re" -> IgnoreCase

    q.toJson shouldBe
      BsonDocument("name" -> BsonDocument("$regex" -> BsonRegularExpression("^test|re", "i"))).toJson
  }

  it should "support multi-value equality" in {
    (prop[ExampleDocument](_.age).in(21 to 25)).toJson shouldBe
      BsonDocument(
        "age" -> BsonDocument(
          "$in" -> BsonArray(
            BsonInt32(21),
            BsonInt32(22),
            BsonInt32(23),
            BsonInt32(24),
            BsonInt32(25)
          )
        )
      ).toJson
  }

  it should "support multi-value inequality" in {
    (!prop[ExampleDocument](_.nested.description).in("hello", "world")).toJson shouldBe
      BsonDocument(
        "nested.description" -> BsonDocument(
          "$nin" -> BsonArray(BsonString("hello"), BsonString("world"))
        )
      ).toJson
  }

  it should "support conjunctions" in {
    val q = prop[ExampleDocument](_.age) < 90 &&
      prop[ExampleDocument](_.nested.score) >= 20.0

    q.toJson shouldBe
      BsonDocument(
        "$and" -> BsonArray(
          BsonDocument("age"          -> BsonDocument("$lt" -> BsonInt32(90))),
          BsonDocument("nested.score" -> BsonDocument("$gte" -> BsonDouble(20.0)))
        )
      ).toJson
  }

  it should "support disjunctions" in {
    val q = prop[ExampleDocument](_.age) < 90 ||
      prop[ExampleDocument](_.nested.score) >= 20.0

    BsonDocument(q.element).toJson shouldBe
      BsonDocument(
        "$or" -> BsonArray(
          BsonDocument("age"          -> BsonDocument("$lt" -> BsonInt32(90))),
          BsonDocument("nested.score" -> BsonDocument("$gte" -> BsonDouble(20.0)))
        )
      ).toJson
  }

  it should "combine adjacent conjunctions" in {
    val q = prop[ExampleDocument](_.age) < 90 &&
      prop[ExampleDocument](_.nested.score) >= 0.0 &&
      prop[ExampleDocument](_.nested.score) < 20.0

    BsonDocument(q.element).toJson shouldBe
      BsonDocument(
        "$and" -> BsonArray(
          BsonDocument("age"          -> BsonDocument("$lt" -> BsonInt32(90))),
          BsonDocument("nested.score" -> BsonDocument("$gte" -> BsonDouble(0.0))),
          BsonDocument(
            "nested.score" ->
              BsonDocument("$lt" -> BsonDouble(20.0))
          )
        )
      ).toJson
  }

  it should "combine adjacent disjunctions" in {
    val q = prop[ExampleDocument](_.age) < 90 ||
      prop[ExampleDocument](_.nested.score) >= 0.0 ||
      prop[ExampleDocument](_.nested.score) < 20.0

    BsonDocument(q.element).toJson shouldBe
      BsonDocument(
        "$or" -> BsonArray(
          BsonDocument("age"          -> BsonDocument("$lt" -> BsonInt32(90))),
          BsonDocument("nested.score" -> BsonDocument("$gte" -> BsonDouble(0.0))),
          BsonDocument(
            "nested.score" ->
              BsonDocument("$lt" -> BsonDouble(20.0))
          )
        )
      ).toJson
  }

  it should "support compound filtering" in {
    val q = prop[ExampleDocument](_.age) < 90 && (
      prop[ExampleDocument](_.nested.score) >= 20.0 ||
        prop[ExampleDocument](_.nested.score).in(0.0, 1.0)
    )

    q.toJson shouldBe
      BsonDocument(
        "$and" -> BsonArray(
          BsonDocument("age" -> BsonDocument("$lt" -> BsonInt32(90))),
          BsonDocument(
            "$or" -> BsonArray(
              BsonDocument("nested.score" -> BsonDocument("$gte" -> BsonDouble(20.0))),
              BsonDocument(
                "nested.score" -> BsonDocument(
                  "$in" -> BsonArray(BsonDouble(0.0), BsonDouble(1.0))
                )
              )
            )
          )
        )
      ).toJson
  }

  it should "support negative existence constraints" in {
    (!prop[ExampleDocument](_.name).exists).toJson shouldBe
      BsonDocument("name" -> BsonDocument("$exists" -> BsonBoolean(false))).toJson
  }

  it should "support 'contains' operation" in {
    prop[ExampleDocument](_.nestedCollection)
      .contains(Nested("", 1, Grandchild("")))
      .toJson shouldBe
      BsonDocument(
        "nestedCollection" -> BsonDocument(
          "$all" -> BsonArray(
            BsonDocument(
              "description" -> "",
              "score"       -> 1.0,
              "grandchild"  -> BsonDocument("saying" -> "")
            )
          )
        )
      ).toJson
  }

  it should "support 'size' operator in form collection.ofSize(size)" in {
    prop[ExampleDocument](_.nestedCollection).ofSize(4).toJson shouldBe
      BsonDocument("nestedCollection" -> BsonDocument("$size" -> 4)).toJson
  }

  it should "support 'size' operator in form collection.size === size" in {
    (prop[ExampleDocument](_.nestedCollection).size === 4).toJson shouldBe
      BsonDocument("nestedCollection" -> BsonDocument("$size" -> 4)).toJson
  }

  it should "support 'push' operator" in {
    prop[ExampleDocument](_.grandchildCollection).push(Grandchild("")).toJson shouldBe
      BsonDocument(
        "$push" -> BsonDocument("grandchildCollection" -> BsonDocument("saying" -> ""))
      ).toJson
  }

  it should "support 'pull' operator with entity parameter" in {
    prop[ExampleDocument](_.grandchildCollection).pull(Grandchild("")).toJson shouldBe
      BsonDocument(
        "$pull" -> BsonDocument("grandchildCollection" -> BsonDocument("saying" -> ""))
      ).toJson
  }

  it should "support 'pull' operator with expression parameter" in {
    prop[ExampleDocument](_.grandchildCollection)
      .pull(prop[Grandchild](_.saying).in(Seq("loud", "silent")))
      .toJson shouldBe
      BsonDocument(
        "$pull" -> BsonDocument(
          "grandchildCollection" -> BsonDocument(
            "saying" -> BsonDocument("$in" -> BsonArray("loud", "silent"))
          )
        )
      ).toJson
  }

  it should "support 'insert' operator" in {
    prop[ExampleDocument](_.grandchildCollection).insert(Grandchild("")).toJson shouldBe
      BsonDocument(
        "$addToSet" -> BsonDocument("grandchildCollection" -> BsonDocument("saying" -> ""))
      ).toJson
  }

  it should "support operations with empty documents" in {
    (Term[Int, Double]("123") === 1.0 && Expression.empty) shouldBe Expression[Int](
      Some("123"),
      BsonElement("123", BsonDouble(1.0))
    )
  }

}
