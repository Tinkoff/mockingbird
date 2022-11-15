package ru.tinkoff.tcb.mockingbird.model

import com.github.dwickern.macros.NameOf.*
import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import io.circe.Json
import io.circe.parser.parse
import kantan.xpath.Node
import kantan.xpath.XmlSource
import sttp.tapir.derevo.schema
import sttp.tapir.generic.Configuration as TapirConfig

import ru.tinkoff.tcb.bson.annotation.BsonDiscriminator
import ru.tinkoff.tcb.bson.derivation.bsonDecoder
import ru.tinkoff.tcb.bson.derivation.bsonEncoder
import ru.tinkoff.tcb.circe.bson.*
import ru.tinkoff.tcb.predicatedsl.json.JsonPredicate
import ru.tinkoff.tcb.predicatedsl.xml.XmlPredicate
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.xml.XMLString

@derive(
  bsonDecoder,
  bsonEncoder,
  decoder(ScenarioInput.modes, true, Some("mode")),
  encoder(ScenarioInput.modes, Some("mode")),
  schema
)
@BsonDiscriminator("mode")
sealed trait ScenarioInput {
  def checkMessage(message: String): Boolean

  def extractJson(message: String): Option[Json]

  def extractXML(message: String): Option[Node]
}

object ScenarioInput {
  val modes: Map[String, String] = Map(
    nameOfType[RawInput]   -> "raw",
    nameOfType[JsonInput]  -> "json",
    nameOfType[XmlInput]   -> "xml",
    nameOfType[JLensInput] -> "jlens",
    nameOfType[XPathInput] -> "xpath"
  ).withDefault(identity)

  implicit val customConfiguration: TapirConfig =
    TapirConfig.default.withDiscriminator("mode").copy(toEncodedName = modes)
}

@derive(decoder, encoder)
final case class RawInput(payload: String) extends ScenarioInput {
  override def checkMessage(message: String): Boolean = message == payload

  override def extractJson(message: String): Option[Json] = None

  override def extractXML(message: String): Option[Node] = None
}

@derive(decoder, encoder)
final case class JsonInput(payload: Json) extends ScenarioInput {
  override def checkMessage(message: String): Boolean =
    parse(message).contains(payload)

  override def extractJson(message: String): Option[Json] =
    parse(message).toOption

  override def extractXML(message: String): Option[Node] = None
}

@derive(decoder, encoder)
final case class XmlInput(payload: XMLString) extends ScenarioInput {
  override def checkMessage(message: String): Boolean =
    XmlSource[String].asNode(message).contains(payload.toKNode)

  override def extractJson(message: String): Option[Json] = None

  override def extractXML(message: String): Option[Node] =
    XmlSource[String].asNode(message).toOption
}

@derive(decoder, encoder)
final case class JLensInput(payload: JsonPredicate) extends ScenarioInput {
  override def checkMessage(message: String): Boolean =
    extractJson(message).map(payload).getOrElse(false)

  override def extractJson(message: String): Option[Json] =
    parse(message).toOption

  override def extractXML(message: String): Option[Node] = None
}

@derive(decoder, encoder)
final case class XPathInput(payload: XmlPredicate) extends ScenarioInput {
  override def checkMessage(message: String): Boolean =
    extractXML(message).map(payload).getOrElse(false)

  override def extractJson(message: String): Option[Json] = None

  override def extractXML(message: String): Option[Node] =
    XmlSource[String].asNode(message).toOption
}
