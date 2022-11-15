package ru.tinkoff.tcb.utils.transformation.json

import java.time.format.DateTimeFormatter
import java.util.UUID

import io.circe.Json
import io.circe.syntax.*
import kantan.xpath.XmlSource
import org.scalatest.OptionValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.utils.circe.*
import ru.tinkoff.tcb.utils.circe.optics.JLens
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic

class JsonTransformationsSpec extends AnyFunSuite with Matchers with OptionValues {
  test("Fill template") {
    val template = Json.obj(
      "description" := "${description}",
      "topic" := "${extras.topic}",
      "comment" := "${extras.comments.[0].text}",
      "meta" := Json.obj(
        "field1" := "${extras.fields.[0]}"
      ),
      "composite" := "${extras.topic}: ${description}"
    )

    val values = Json.obj(
      "description" := "Some description",
      "extras" := Json.obj(
        "fields" := "f1" :: "f2" :: Nil,
        "topic" := "Main topic",
        "comments" := Json.obj("text" := "First nah!") :: Json.obj("text" := "Okay") :: Nil
      )
    )

    val sut = template.substitute(values)

    sut shouldBe Json.obj(
      "description" := "Some description",
      "topic" := "Main topic",
      "comment" := "First nah!",
      "meta" := Json.obj(
        "field1" := "f1"
      ),
      "composite" := "Main topic: Some description"
    )
  }

  test("Absent fields") {
    val template = Json.obj(
      "value" := "${description}"
    )

    val sut = template.substitute(Json.obj())

    sut shouldBe template
  }

  test("Substitute object") {
    val template = Json.obj("value" := "${message}")

    val sut = template.substitute(Json.obj("message" := Json.obj("peka" := "yoba")))

    sut shouldBe Json.obj("value" := Json.obj("peka" := "yoba"))
  }

  test("Convert to string") {
    val template = Json.obj(
      "a" := "$:{b1}",
      "b" := "$:{b2}",
      "c" := "$:{n}"
    )

    val values = Json.obj(
      "b1" := true,
      "b2" := false,
      "n" := 45.99
    )

    val sut = template.substitute(values)

    sut shouldBe Json.obj(
      "a" := "true",
      "b" := "false",
      "c" := "45.99"
    )
  }

  test("Convert from string") {
    val template = Json.obj(
      "a" := "$~{b1}",
      "b" := "$~{b2}",
      "c" := "$~{n}"
    )

    val values = Json.obj(
      "b1" := "true",
      "b2" := "false",
      "n" := "45.99"
    )

    val sut = template.substitute(values)

    sut shouldBe Json.obj(
      "a" := true,
      "b" := false,
      "c" := 45.99
    )
  }

  private def xml(str: String) = XmlSource[String].asUnsafeNode(str)

  test("Fill template from XML") {
    val template = Json.obj(
      "value1" := "${/root/tag1}",
      "value2" := "${/root/tag2}"
    )

    val data = xml("<root><tag1>test</tag1><tag2>42</tag2></root>")

    val sut = template.substitute(data)

    sut shouldBe Json.obj(
      "value1" := "test",
      "value2" := "42"
    )
  }

  test("Failover test") {
    Json.Null.substitute(Json.Null)
    Json.Null.substitute(Json.obj())
    Json.obj().substitute(Json.Null)
    Json.obj().substitute(Json.obj())
  }

  test("Simple eval") {
    val datePattern = "yyyy-MM-dd"
    val dFormatter  = DateTimeFormatter.ofPattern(datePattern)
    val pattern     = "yyyy-MM-dd'T'HH:mm:ss"
    val formatter   = DateTimeFormatter.ofPattern(pattern)

    val template = Json.obj(
      "a" := "%{randomString(10)}",
      "ai" := "%{randomString(\"ABCDEF1234567890\", 4, 6)}",
      "b" := "%{randomInt(5)}",
      "bi" := "%{randomInt(3, 8)}",
      "c" := "%{randomLong(5)}",
      "ci" := "%{randomLong(3, 8)}",
      "d" := "%{UUID}",
      "e" := s"%{now($pattern)}",
      "f" := s"%{today($datePattern)}"
    )

    val res = template.eval

    (res \\ "a").headOption.flatMap(_.asString).value should have length 10

    info((res \\ "ai").headOption.flatMap(_.asString).value)
    (res \\ "ai").headOption.flatMap(_.asString).value should fullyMatch regex """[ABCDEF1234567890]{4,5}"""

    val b = (res \\ "b").headOption.flatMap(_.asNumber).flatMap(_.toInt).value
    b should be >= 0
    b should be < 5

    val bi = (res \\ "bi").headOption.flatMap(_.asNumber).flatMap(_.toInt).value
    bi should be >= 3
    bi should be < 8

    val c = (res \\ "c").headOption.flatMap(_.asNumber).flatMap(_.toLong).value
    c should be >= 0L
    c should be < 5L

    val ci = (res \\ "ci").headOption.flatMap(_.asNumber).flatMap(_.toLong).value
    ci should be >= 3L
    ci should be < 8L

    val d = (res \\ "d").headOption.flatMap(_.asString).value
    noException should be thrownBy UUID.fromString(d)

    val e = (res \\ "e").headOption.flatMap(_.asString).value
    noException should be thrownBy formatter.parse(e)

    val f = (res \\ "f").headOption.flatMap(_.asString).value
    noException should be thrownBy dFormatter.parse(f)
  }

  test("Formatted eval") {
    val template = Json.obj(
      "fmt" := "%{randomInt(10)}: %{randomLong(10)} | %{randomString(12)}"
    )

    val res = template.eval

    (res \\ "fmt").headOption.flatMap(_.asString).value should have length 19
  }

  test("Json patcher") {
    val target = Json.obj(
      "f1" := "v1",
      "a2" := "e1" :: "e2" :: "e3" :: Nil,
      "o3" := Json.obj()
    )

    val source = Json.obj(
      "name" := "Peka",
      "surname" := "Kekovsky",
      "comment" := "nondesc"
    )

    val schema = Map(
      JsonOptic.fromPathString("a2.[4]")    -> "${comment}",
      JsonOptic.fromPathString("o3.client") -> "${name} ${surname}"
    )

    val sut = target.patch(source, schema)

    sut.get(JLens \ "a2" \ 4).asString.value shouldBe "nondesc"
    sut.get(JLens \ "o3" \ "client").asString.value shouldBe "Peka Kekovsky"
  }
}
