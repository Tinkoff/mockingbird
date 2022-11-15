package ru.tinkoff.tcb.mockingbird.model

import com.github.dwickern.macros.NameOf.*
import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import io.circe.Json
import kantan.xpath.*
import kantan.xpath.implicits.*
import sttp.tapir.derevo.schema
import sttp.tapir.generic.Configuration as TapirConfig

import ru.tinkoff.tcb.bson.annotation.BsonDiscriminator
import ru.tinkoff.tcb.bson.derivation.bsonDecoder
import ru.tinkoff.tcb.bson.derivation.bsonEncoder
import ru.tinkoff.tcb.protocol.bson.*
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.protocol.xml.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.xpath.Xpath

@derive(
  bsonDecoder,
  bsonEncoder,
  decoder(XmlExtractor.types, true, Some("type")),
  encoder(XmlExtractor.types, Some("type")),
  schema
)
@BsonDiscriminator("type")
sealed trait XmlExtractor {
  def apply(node: Node): Either[XPathError, Json]
}
object XmlExtractor {
  val types: Map[String, String] = Map(
    nameOfType[XMLCDataExtractor]  -> "xcdata",
    nameOfType[JsonCDataExtractor] -> "jcdata",
  ).withDefault(identity)

  implicit val customConfiguration: TapirConfig =
    TapirConfig.default.withDiscriminator("type").copy(toEncodedName = types)
}

/**
 * @param prefix
 *   Путь до CDATA
 * @param path
 *   Путь внутри CDATA
 */
@derive(decoder, encoder)
case class XMLCDataExtractor(prefix: Xpath, path: Xpath) extends XmlExtractor {
  def apply(node: Node): Either[XPathError, Json] =
    node
      .evalXPath[String](prefix.toXPathExpr)
      .flatMap(_.trim().asNode)
      .flatMap(_.evalXPath[String](path.toXPathExpr))
      .map(Json.fromString)
}

/**
 * @param prefix
 *   Путь до CDATA
 * @param path
 *   Путь внутри CDATA
 */
@derive(decoder, encoder)
case class JsonCDataExtractor(prefix: Xpath, path: JsonOptic) extends XmlExtractor {
  def apply(node: Node): Either[XPathError, Json] =
    node.evalXPath[Json](prefix.toXPathExpr).map(path.get)
}
