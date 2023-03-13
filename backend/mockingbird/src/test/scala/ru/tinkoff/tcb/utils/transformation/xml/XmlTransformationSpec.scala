package ru.tinkoff.tcb.utils.transformation.xml

import scala.xml.Node

import advxml.transform.XmlZoom
import advxml.xpath.*
import io.circe.Json
import io.circe.syntax.*
import kantan.xpath.XmlSource
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.utils.xml.SafeXML

class XmlTransformationSpec extends AnyFunSuite with Matchers {
  private def xml(str: String) = XmlSource[String].asUnsafeNode(str)

  test("Fill template from KNode") {
    val template: Node =
      <root rt="${/root/rt}"><tag1 t1="a1">${{/root/tag1}}</tag1><tag2 t2="${root/tag2/@t2}">${{/root/tag2}}</tag2></root>

    val data = xml("<root><tag1>test</tag1><tag2 t2=\"a2\">42</tag2><rt>kek</rt></root>")

    val sut = template.substitute(data)

    sut shouldBe <root rt="kek"><tag1 t1="a1">test</tag1><tag2 t2="a2">42</tag2></root>
  }

  test("Fill template from Node") {
    val template: Node =
      <root rt="${/root/rt}"><tag1 t1="a1">${{/root/tag1}}</tag1><tag2 t2="${root/tag2}">${{/root/tag2}}</tag2></root>

    val data: Node = <wrapper><root><tag1>test</tag1><tag2 t2="a2">42</tag2><rt>kek</rt></root></wrapper>

    val sut = template.substitute(data)

    sut shouldBe <root rt="kek"><tag1 t1="a1">test</tag1><tag2 t2="42">42</tag2></root>
  }

  test("Fill template from JSON") {
    val template: Node =
      <root rt="${root.rt}"><tag1 t1="a1">${{root.tag1}}</tag1><tag2 t2="${root.t2}">${{root.tag2}}</tag2><composite cmp="${root.rt}_${root.t2}">${{root.tag1}}_${{root.tag2}}</composite></root>

    val data = Json.obj(
      "root" := Json.obj(
        "tag1" := "test",
        "tag2" := 42,
        "rt" := "kek",
        "t2" := "a2"
      )
    )

    val sut = template.substitute(data)

    sut shouldBe <root rt="kek"><tag1 t1="a1">test</tag1><tag2 t2="a2">42</tag2><composite cmp="kek_a2">test_42</composite></root>
  }

  test("Eval template") {
    val template: Node =
      <root>
        <tag1>%{{randomString(10)}}</tag1>
        <tag1i>%{{randomString("ABCDEF1234567890", 4, 6)}}</tag1i>
        <tag2>%{{randomInt(5)}}</tag2>
        <tag2i>%{{randomInt(3, 8)}}</tag2i>
        <tag3>%{{randomLong(5)}}</tag3>
        <tag3i>%{{randomLong(3, 8)}}</tag3i>
        <tag4>%{{UUID}}</tag4>
      </root>

    template.isTemplate shouldBe true

    val res = template.eval

    (res \ "tag1").text should have length 10

    (res \ "tag1i").text should fullyMatch regex """[ABCDEF1234567890]{4,5}"""

    (res \ "tag2").text.toInt should be >= 0
    (res \ "tag2").text.toInt should be < 5

    (res \ "tag2i").text.toInt should be >= 3
    (res \ "tag2i").text.toInt should be < 8

    (res \ "tag3").text.toLong should be >= 0L
    (res \ "tag3").text.toLong should be < 5L

    (res \ "tag3i").text.toLong should be >= 3L
    (res \ "tag3i").text.toLong should be < 8L
  }

  test("Inline CDATA") {
    val template1: Node = SafeXML.loadString("<root><![CDATA[%s]]></root>".format("<tag1>42</tag1>"))
    val template2: Node = SafeXML.loadString("<root> <![CDATA[%s]]></root>".format("<tag1>42</tag1>"))
    val template3: Node = SafeXML.loadString("<root><![CDATA[%s]]> </root>".format("<tag1>42</tag1>"))

    val res1 = template1.inlineXmlFromCData
    val res2 = template2.inlineXmlFromCData
    val res3 = template3.inlineXmlFromCData

    (res1 \ "tag1").text shouldBe "42"
    (res2 \ "tag1").text shouldBe "42"
    (res3 \ "tag1").text shouldBe "42"
  }

  test("XML patcher") {
    val target: Node =
      <root>
        <inner>test</inner>
        <second>test</second>
      </root>

    val source = Json.obj(
      "name" := "Peka",
      "surname" := "Kekovsky",
      "comment" := "nondesc"
    )

    val xSource: Node =
      <data>
        <value>this</value>
      </data>

    val schema = Map(
      XmlZoom.fromXPath("/root/inner").toOption.get  -> "${comment}",
      XmlZoom.fromXPath("/root/second").toOption.get -> "${/data/value}"
    )

    val sut = target.patchFromValues(source, xSource, schema)

    info(sut.toString())

    (sut \ "inner").text shouldBe "nondesc"
    (sut \ "second").text shouldBe "this"
  }

  test("Template checker") {
    val simpleNode: Node =
      <root>
        <inner>test</inner>
        <second>test</second>
      </root>

    simpleNode.isTemplate shouldBe false

    val template1: Node =
      <root rt="${/root/rt}">
      </root>

    template1.isTemplate shouldBe true

    val template2: Node =
      <root rt="${root.rt}_${root.t2}">
      </root>

    template2.isTemplate shouldBe true

    val template3: Node =
      <root>
        <tag1 t1="a1">${{/root/tag1}}</tag1>
      </root>

    template3.isTemplate shouldBe true

    val template4: Node =
      <root>
        <tag1 t1="a1">${{root.tag1}}_${{root.tag2}}</tag1>
      </root>

    template4.isTemplate shouldBe true
  }
}
