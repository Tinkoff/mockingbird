package ru.tinkoff.tcb.predicatedsl.xml

import cats.data.NonEmptyList
import cats.scalatest.EitherValues
import io.circe.Json
import io.circe.syntax.*
import kantan.xpath.XmlSource
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.predicatedsl.SpecificationError
import ru.tinkoff.tcb.xpath.*

class XmlPredicateSpec extends AnyFunSuite with Matchers with EitherValues {
  private def xml(str: String) = XmlSource[String].asUnsafeNode(str)

  test("XmlPredicate should produce validator from correct specification") {
    val spec = Json.obj(
      "/tag1" := Json.obj("==" := "test")
    )

    val sut = XmlPredicate(spec.as[Map[Xpath, Map[Keyword.Xml, Json]]].value).toEither

    sut shouldBe Symbol("right")
  }

  test("XmlPredicate should emit correct error for poor specification") {
    val spec = Json.obj(
      "/tag1" := Json.obj(">=" := "test")
    )

    val sut = XmlPredicate(spec.as[Map[Xpath, Map[Keyword.Xml, Json]]].value).toEither

    sut shouldBe Left(
      NonEmptyList.one(
        SpecificationError("/tag1", NonEmptyList.one(Keyword.Gte.asInstanceOf[Keyword] -> Json.fromString("test")))
      )
    )
  }

  test("Check equality") {
    val spec = Json.obj(
      "/root/tag1" := Json.obj("==" := "test"),
      "/root/tag2" := Json.obj("==" := 42)
    )

    val sut = XmlPredicate(spec.as[Map[Xpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(xml("<root><tag1>test</tag1><tag2>42</tag2></root>"))) shouldBe Some(true)
    sut.map(_(xml("<root><tag1>peka</tag1><tag2>42</tag2></root>"))) shouldBe Some(false)
  }

  test("Check inequality") {
    val spec = Json.obj(
      "/root/tag1" := Json.obj("!=" := "test"),
      "/root/tag2" := Json.obj("!=" := 42)
    )

    val sut = XmlPredicate(spec.as[Map[Xpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(xml("<root><tag1>test</tag1><tag2>42</tag2></root>"))) shouldBe Some(false)
    sut.map(_(xml("<root><tag1>peka</tag1><tag2>99</tag2></root>"))) shouldBe Some(true)
  }

  test("Check >") {
    val spec = Json.obj(
      "/f" := Json.obj(">" := 42)
    )

    val sut = XmlPredicate(spec.as[Map[Xpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(xml("<f>43</f>"))) shouldBe Some(true)
    sut.map(_(xml("<f>42</f>"))) shouldBe Some(false)
  }

  test("Check >=") {
    val spec = Json.obj(
      "/f" := Json.obj(">=" := 42)
    )

    val sut = XmlPredicate(spec.as[Map[Xpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(xml("<f>43</f>"))) shouldBe Some(true)
    sut.map(_(xml("<f>42</f>"))) shouldBe Some(true)
    sut.map(_(xml("<f>41</f>"))) shouldBe Some(false)
  }

  test("Check <") {
    val spec = Json.obj(
      "/f" := Json.obj("<" := 42)
    )

    val sut = XmlPredicate(spec.as[Map[Xpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(xml("<f>42</f>"))) shouldBe Some(false)
    sut.map(_(xml("<f>41</f>"))) shouldBe Some(true)
  }

  test("Check <=") {
    val spec = Json.obj(
      "/f" := Json.obj("<=" := 42)
    )

    val sut = XmlPredicate(spec.as[Map[Xpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(xml("<f>43</f>"))) shouldBe Some(false)
    sut.map(_(xml("<f>42</f>"))) shouldBe Some(true)
    sut.map(_(xml("<f>41</f>"))) shouldBe Some(true)
  }

  test("Check range") {
    val spec = Json.obj(
      "/f" := Json.obj(">" := 40, "<=" := 45, "!=" := 43)
    )

    val sut = XmlPredicate(spec.as[Map[Xpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(xml("<f>39</f>"))) shouldBe Some(false)
    sut.map(_(xml("<f>40</f>"))) shouldBe Some(false)
    sut.map(_(xml("<f>41</f>"))) shouldBe Some(true)
    sut.map(_(xml("<f>42</f>"))) shouldBe Some(true)
    sut.map(_(xml("<f>43</f>"))) shouldBe Some(false)
    sut.map(_(xml("<f>44</f>"))) shouldBe Some(true)
    sut.map(_(xml("<f>45</f>"))) shouldBe Some(true)
    sut.map(_(xml("<f>46</f>"))) shouldBe Some(false)
  }

  test("Check regex match") {
    val spec = Json.obj(
      "f" := Json.obj("~=" := "\\d{4,}")
    )

    val sut = XmlPredicate(spec.as[Map[Xpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(xml("<f>123</f>"))) shouldBe Some(false)
    sut.map(_(xml("<f>1234</f>"))) shouldBe Some(true)
    sut.map(_(xml("<f>1234a</f>"))) shouldBe Some(false)
    sut.map(_(xml("<f>12345</f>"))) shouldBe Some(true)
  }

  test("Check size") {
    val spec = Json.obj(
      "/f" := Json.obj("size" := 4)
    )

    val sut = XmlPredicate(spec.as[Map[Xpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(xml("<f>123</f>"))) shouldBe Some(false)
    sut.map(_(xml("<f>1234</f>"))) shouldBe Some(true)
    sut.map(_(xml("<f>1234a</f>"))) shouldBe Some(false)
  }

  test("Check exists") {
    val spec = Json.obj(
      "/f" := Json.obj("exists" := true)
    )

    val sut = XmlPredicate(spec.as[Map[Xpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(xml("<f>123</f>"))) shouldBe Some(true)
    sut.map(_(xml("<f/>"))) shouldBe Some(true)
    sut.map(_(xml("<g>123</g>"))) shouldBe Some(false)
  }

  test("Check not exists") {
    val spec = Json.obj(
      "/f" := Json.obj("exists" := false)
    )

    val sut = XmlPredicate(spec.as[Map[Xpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(xml("<f>123</f>"))) shouldBe Some(false)
    sut.map(_(xml("<f/>"))) shouldBe Some(false)
    sut.map(_(xml("<g>123</g>"))) shouldBe Some(true)
  }

  test("CDATA equals") {
    val spec = Json.obj(
      "/data" := Json.obj(
        "cdata" := Json.obj(
          "==" := "test"
        )
      )
    )

    val sut = XmlPredicate(spec.as[Map[Xpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(xml("""<data><![CDATA[test]]></data>"""))) shouldBe Some(true)
    sut.map(_(xml("""<data><![CDATA[test]]> </data>"""))) shouldBe Some(false)
    sut.map(_(xml("""<data><![CDATA[kek]]></data>"""))) shouldBe Some(false)
  }

  test("Check CDATA with regex") {
    val spec = Json.obj(
      "/data" := Json.obj(
        "cdata" := Json.obj(
          "~=" := "\\d+"
        )
      )
    )

    val sut = XmlPredicate(spec.as[Map[Xpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(xml("""<data><![CDATA[1234]]></data>"""))) shouldBe Some(true)
    sut.map(_(xml("""<data><![CDATA[123f]]></data>"""))) shouldBe Some(false)
    sut.map(_(xml("""<data><![CDATA[1234]]> </data>"""))) shouldBe Some(false)
  }

  test("Check CDATA with JSON") {
    val spec = Json.obj(
      "/json" := Json.obj(
        "jcdata" := Json.obj(
          "f" := Json.obj(
            "==" := 42
          )
        )
      )
    )

    val sut = XmlPredicate(spec.as[Map[Xpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(xml("""<json> <![CDATA[{"f": 42}]]> </json>"""))) shouldBe Some(true)
    sut.map(_(xml("""<json> <![CDATA[{"f": 43}]]> </json>"""))) shouldBe Some(false)
    sut.map(_(xml("""<json> <![CDATA[{"f": 42]]> </json>"""))) shouldBe Some(false)
  }

  test("Check CDATA with XML") {
    val spec = Json.obj(
      "/xml" := Json.obj(
        "xcdata" := Json.obj(
          "/f" := Json.obj(
            "==" := 42
          )
        )
      )
    )

    val sut = XmlPredicate(spec.as[Map[Xpath, Map[Keyword.Xml, Json]]].value).toOption

    sut.map(_(xml("""<xml> <![CDATA[<f>42</f>]]> </xml>"""))) shouldBe Some(true)
    sut.map(_(xml("""<xml> <![CDATA[<f>43</f>]]> </xml>"""))) shouldBe Some(false)
    sut.map(_(xml("""<xml> <![CDATA[<f>42</f]]> </xml>"""))) shouldBe Some(false)
  }
}
