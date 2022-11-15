package ru.tinkoff.tcb.utils

import java.io.StringWriter
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import scala.util.Try
import scala.xml.Node

import io.circe.Decoder
import io.circe.Encoder
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops.*
import kantan.xpath.Node as KNode
import kantan.xpath.XmlSource
import mouse.boolean.*
import sttp.tapir.Schema

import ru.tinkoff.tcb.bson.BsonDecoder
import ru.tinkoff.tcb.bson.BsonEncoder
import ru.tinkoff.tcb.generic.RootOptionFields

package object xml {
  val emptyKNode: KNode = XmlSource[String].asUnsafeNode("<empty />")

  @newtype class XMLString private (val asString: String)

  object XMLString {
    def fromString(xml: String): Try[XMLString] = XmlSource[String].asNode(xml).toTry.as(xml.coerce)

    def fromNode(node: Node): XMLString = node.mkString.coerce

    def unapply(str: String): Option[XMLString] =
      fromString(str).toOption

    implicit val xmlStringDecoder: Decoder[XMLString] =
      Decoder.decodeString.emapTry(fromString)

    implicit val xmlStringEncoder: Encoder[XMLString] =
      Encoder.encodeString.coerce

    /*
      Для ускорения чтения здесь не производится валидация,
      предполагается, что данные записаны уже провалидированными
     */
    implicit val xmlStringBsonDecoder: BsonDecoder[XMLString] =
      BsonDecoder[String].coerce

    implicit val xmlStringBsonEncoder: BsonEncoder[XMLString] =
      BsonEncoder[String].coerce

    implicit val xmlStringSchema: Schema[XMLString] =
      Schema.schemaForString.as[XMLString]

    implicit val xmlStringRof: RootOptionFields[XMLString] =
      RootOptionFields.mk(Set.empty)
  }

  implicit class XMLStringSyntax(private val self: XMLString) extends AnyVal {
    def toNode: Node   = SafeXML.loadString(self.asString)
    def toKNode: KNode = XmlSource[String].asUnsafeNode(self.asString)
  }

  implicit class NodePrinter(private val node: KNode) extends AnyVal {
    def print(pretty: Boolean = true, omitXMLDeclaration: Boolean = true): String = {
      val writer = new StringWriter()
      val t      = TransformerFactory.newInstance().newTransformer()
      t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXMLDeclaration.fold("yes", "no"))
      t.setOutputProperty(OutputKeys.INDENT, pretty.fold("yes", "no"))
      t.transform(new DOMSource(node), new StreamResult(writer))
      writer.toString
    }
  }
}
