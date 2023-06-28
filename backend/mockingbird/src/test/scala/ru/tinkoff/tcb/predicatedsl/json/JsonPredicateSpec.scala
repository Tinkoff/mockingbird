package ru.tinkoff.tcb.predicatedsl.json

import cats.data.NonEmptyList
import cats.scalatest.EitherValues
import io.circe.Json
import io.circe.syntax.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.predicatedsl.JSpecificationError
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic

class JsonPredicateSpec extends AnyFunSuite with Matchers with EitherValues {
  test("JsonPredicate should produce validator from correct specification") {
    val spec = Json.obj(
      "field1" := Json.obj("==" := "test")
    )

    val sut = JsonPredicate(spec.as[Map[JsonOptic, Map[Keyword.Json, Json]]].value).toEither

    sut shouldBe Symbol("right")
  }

  test("JsonPredicate should emit correct error for poor specification") {
    val spec = Json.obj(
      "field1" := Json.obj(">=" := "test")
    )

    val sut = JsonPredicate(spec.as[Map[JsonOptic, Map[Keyword.Json, Json]]].value).toEither

    sut shouldBe Left(
      NonEmptyList.one(
        JSpecificationError(
          JsonOptic.forPath("field1"),
          NonEmptyList.one(Keyword.Gte.asInstanceOf[Keyword] -> Json.fromString("test"))
        )
      )
    )
  }

  test("Check equality") {
    val spec = Json.obj(
      "field1" := Json.obj("==" := "test"),
      "field2" := Json.obj("==" := 1 :: 2 :: 3 :: Nil),
      "field3" := Json.obj("==" := Json.obj("name" := "peka"))
    )

    val sut = JsonPredicate(spec.as[Map[JsonOptic, Map[Keyword.Json, Json]]].value).toOption

    sut.map(
      _(
        Json.obj(
          "field1" := "test",
          "field2" := 1 :: 2 :: 3 :: Nil,
          "field3" := Json.obj("name" := "peka")
        )
      )
    ) shouldBe Some(true)

    sut.map(
      _(
        Json.obj(
          "field1" := "peka",
          "field2" := 1 :: 2 :: 3 :: Nil,
          "field3" := Json.obj("name" := "peka")
        )
      )
    ) shouldBe Some(false)
  }

  test("Check inequality") {
    val spec = Json.obj(
      "field1" := Json.obj("!=" := "test"),
      "field2" := Json.obj("==" := 1 :: 2 :: 3 :: Nil),
      "field3" := Json.obj("==" := Json.obj("name" := "peka"))
    )

    val sut = JsonPredicate(spec.as[Map[JsonOptic, Map[Keyword.Json, Json]]].value).toOption

    sut.map(
      _(
        Json.obj(
          "field1" := "name",
          "field2" := 1 :: 2 :: 3 :: Nil,
          "field3" := Json.obj("name" := "peka")
        )
      )
    ) shouldBe Some(true)

    sut.map(
      _(
        Json.obj(
          "field1" := "test",
          "field2" := 1 :: 2 :: 3 :: Nil,
          "field3" := Json.obj("name" := "peka")
        )
      )
    ) shouldBe Some(false)
  }

  test("Check >") {
    val spec = Json.obj(
      "f" := Json.obj(">" := 42)
    )

    val sut = JsonPredicate(spec.as[Map[JsonOptic, Map[Keyword.Json, Json]]].value).toOption

    sut.map(_(Json.obj("f" := 43))) shouldBe Some(true)
    sut.map(_(Json.obj("f" := 42))) shouldBe Some(false)
  }

  test("Check >=") {
    val spec = Json.obj(
      "f" := Json.obj(">=" := 42)
    )

    val sut = JsonPredicate(spec.as[Map[JsonOptic, Map[Keyword.Json, Json]]].value).toOption

    sut.map(_(Json.obj("f" := 43))) shouldBe Some(true)
    sut.map(_(Json.obj("f" := 42))) shouldBe Some(true)
    sut.map(_(Json.obj("f" := 41))) shouldBe Some(false)
  }

  test("Check <") {
    val spec = Json.obj(
      "f" := Json.obj("<" := 42)
    )

    val sut = JsonPredicate(spec.as[Map[JsonOptic, Map[Keyword.Json, Json]]].value).toOption

    sut.map(_(Json.obj("f" := 41))) shouldBe Some(true)
    sut.map(_(Json.obj("f" := 42))) shouldBe Some(false)
  }

  test("Check <=") {
    val spec = Json.obj(
      "f" := Json.obj("<=" := 42)
    )

    val sut = JsonPredicate(spec.as[Map[JsonOptic, Map[Keyword.Json, Json]]].value).toOption

    sut.map(_(Json.obj("f" := 41))) shouldBe Some(true)
    sut.map(_(Json.obj("f" := 42))) shouldBe Some(true)
    sut.map(_(Json.obj("f" := 43))) shouldBe Some(false)
  }

  test("Check range") {
    val spec = Json.obj(
      "f" := Json.obj(">" := 40, "<=" := 45, "!=" := 43)
    )

    val sut = JsonPredicate(spec.as[Map[JsonOptic, Map[Keyword.Json, Json]]].value).toOption

    sut.map(_(Json.obj("f" := 39))) shouldBe Some(false)
    sut.map(_(Json.obj("f" := 40))) shouldBe Some(false)
    sut.map(_(Json.obj("f" := 41))) shouldBe Some(true)
    sut.map(_(Json.obj("f" := 42))) shouldBe Some(true)
    sut.map(_(Json.obj("f" := 43))) shouldBe Some(false)
    sut.map(_(Json.obj("f" := 44))) shouldBe Some(true)
    sut.map(_(Json.obj("f" := 45))) shouldBe Some(true)
    sut.map(_(Json.obj("f" := 46))) shouldBe Some(false)
  }

  test("Check regex match") {
    val spec = Json.obj(
      "f" := Json.obj("~=" := "\\d{4,}")
    )

    val sut = JsonPredicate(spec.as[Map[JsonOptic, Map[Keyword.Json, Json]]].value).toOption

    sut.map(_(Json.obj("f" := "123"))) shouldBe Some(false)
    sut.map(_(Json.obj("f" := "1234"))) shouldBe Some(true)
    sut.map(_(Json.obj("f" := 1234))) shouldBe Some(false)
    sut.map(_(Json.obj("f" := "1234a"))) shouldBe Some(false)
    sut.map(_(Json.obj("f" := "12345"))) shouldBe Some(true)
  }

  test("Check size") {
    val spec = Json.obj(
      "f" := Json.obj("size" := 4)
    )

    val sut = JsonPredicate(spec.as[Map[JsonOptic, Map[Keyword.Json, Json]]].value).toOption

    sut.map(_(Json.obj("f" := "1234"))) shouldBe Some(true)
    sut.map(_(Json.obj("f" := 1 :: 2 :: 3 :: 4 :: Nil))) shouldBe Some(true)
    sut.map(_(Json.obj("f" := 1234))) shouldBe Some(false)
    sut.map(_(Json.obj("f" := 4))) shouldBe Some(false)
  }

  test("Check exists") {
    val spec = Json.obj(
      "f" := Json.obj("exists" := Some(true))
    )

    val sut = JsonPredicate(spec.as[Map[JsonOptic, Map[Keyword.Json, Json]]].value).toOption

    sut.map(_(Json.obj("f" := 42))) shouldBe Some(true)
    sut.map(_(Json.obj("f" := Json.Null))) shouldBe Some(false)
    sut.map(_(Json.obj())) shouldBe Some(false)
  }

  test("Check not exists") {
    val spec = Json.obj(
      "f" := Json.obj("exists" := false)
    )

    val sut = JsonPredicate(spec.as[Map[JsonOptic, Map[Keyword.Json, Json]]].value).toOption

    sut.map(_(Json.obj("f" := 42))) shouldBe Some(false)
    sut.map(_(Json.obj("f" := Json.Null))) shouldBe Some(true)
    sut.map(_(Json.obj())) shouldBe Some(true)
  }

  test("Check [_]") {
    val spec = Json.obj(
      "f" := Json.obj("[_]" := "1".asJson :: 2.asJson :: true.asJson :: Nil)
    )

    val sut = JsonPredicate(spec.as[Map[JsonOptic, Map[Keyword.Json, Json]]].value).toOption

    sut.map(_(Json.obj("f" := "1"))) shouldBe Some(true)
    sut.map(_(Json.obj("f" := 2))) shouldBe Some(true)
    sut.map(_(Json.obj("f" := true))) shouldBe Some(true)
    sut.map(_(Json.obj("f" := "2"))) shouldBe Some(false)
    sut.map(_(Json.obj("f" := 1))) shouldBe Some(false)
    sut.map(_(Json.obj("f" := false))) shouldBe Some(false)
    sut.map(_(Json.obj("f" := Vector.empty[Json]))) shouldBe Some(false)
    sut.map(_(Json.obj())) shouldBe Some(false)
  }

  test("Check ![_]") {
    val spec = Json.obj(
      "f" := Json.obj("![_]" := "1".asJson :: 2.asJson :: true.asJson :: Nil)
    )

    val sut = JsonPredicate(spec.as[Map[JsonOptic, Map[Keyword.Json, Json]]].value).toOption

    sut.map(_(Json.obj("f" := "1"))) shouldBe Some(false)
    sut.map(_(Json.obj("f" := 2))) shouldBe Some(false)
    sut.map(_(Json.obj("f" := true))) shouldBe Some(false)
    sut.map(_(Json.obj("f" := "2"))) shouldBe Some(true)
    sut.map(_(Json.obj("f" := 1))) shouldBe Some(true)
    sut.map(_(Json.obj("f" := false))) shouldBe Some(true)
    sut.map(_(Json.obj("f" := Vector.empty[Json]))) shouldBe Some(true)
    sut.map(_(Json.obj())) shouldBe Some(true)
  }

  test("Check &[_]") {
    val spec = Json.obj(
      "f" := Json.obj("&[_]" := "1".asJson :: 2.asJson :: true.asJson :: Nil)
    )

    val sut = JsonPredicate(spec.as[Map[JsonOptic, Map[Keyword.Json, Json]]].value).toOption

    sut.map(_(Json.obj("f" := 1))) shouldBe Some(false)
    sut.map(_(Json.obj("f" := "1"))) shouldBe Some(false)
    sut.map(_(Json.obj("f" := "1".asJson :: 2.asJson :: true.asJson :: Nil))) shouldBe Some(true)
    sut.map(_(Json.obj("f" := 2.asJson :: "1".asJson :: true.asJson :: Nil))) shouldBe Some(true)
    sut.map(_(Json.obj("f" := 2.asJson :: "1".asJson :: false.asJson :: Nil))) shouldBe Some(false)
  }
}
