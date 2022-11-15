package ru.tinkoff.tcb.mockingbird.model

import scala.util.Try

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
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.predicatedsl.form.FormPredicate
import ru.tinkoff.tcb.predicatedsl.json.JsonPredicate
import ru.tinkoff.tcb.predicatedsl.xml.XmlPredicate
import ru.tinkoff.tcb.protocol.bson.*
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.utils.transformation.xml.*
import ru.tinkoff.tcb.utils.webform.decode as decodeForm
import ru.tinkoff.tcb.utils.webform.toJson
import ru.tinkoff.tcb.utils.xml.SafeXML
import ru.tinkoff.tcb.utils.xml.XMLString

@derive(
  bsonDecoder,
  bsonEncoder,
  decoder(HttpStubRequest.modes, true, Some("mode")),
  encoder(HttpStubRequest.modes, Some("mode")),
  schema
)
@BsonDiscriminator("mode")
sealed trait HttpStubRequest {
  def headers: Map[String, String]

  def query: Map[JsonOptic, Map[Keyword.Json, Json]]

  def checkHeaders(hs: Map[String, String]): Boolean =
    headers.forall { case (k, v) =>
      hs.exists {
        case (kx, vx) => k.toLowerCase == kx.toLowerCase && v == vx
        case _        => false
      }
    }

  def checkQueryParams(params: Json): Boolean =
    if (query.isEmpty) true else JsonPredicate(query).map(_(params)).toOption.getOrElse(false)

  def checkStringBody(bodyStr: String): Boolean

  def extractJson(bodyStr: String): Option[Json]

  def extractXML(bodyStr: String): Option[Node]

  def runXmlExtractors(body: Node): Json
}
object HttpStubRequest {
  val modes: Map[String, String] = Map(
    nameOfType[JsonRequest]        -> "json",
    nameOfType[XmlRequest]         -> "xml",
    nameOfType[RawRequest]         -> "raw",
    nameOfType[JLensRequest]       -> "jlens",
    nameOfType[XPathRequest]       -> "xpath",
    nameOfType[RequestWithoutBody] -> "no_body",
    nameOfType[RequestWithAnyBody] -> "any_body",
    nameOfType[WebFormRequest]     -> "web_form"
  ).withDefault(identity)

  implicit val customConfiguration: TapirConfig =
    TapirConfig.default.withDiscriminator("mode").copy(toEncodedName = modes)
}

@derive(decoder, encoder)
final case class JsonRequest(
    headers: Map[String, String],
    query: Map[JsonOptic, Map[Keyword.Json, Json]] = Map.empty,
    body: Json
) extends HttpStubRequest {
  override def checkStringBody(bodyStr: String): Boolean =
    parse(bodyStr).contains(body)

  override def extractJson(bodyStr: String): Option[Json] =
    parse(bodyStr).toOption

  override def extractXML(bodyStr: String): Option[Node] = None

  override def runXmlExtractors(body: Node): Json = Json.Null
}

@derive(decoder, encoder)
final case class XmlRequest(
    headers: Map[String, String],
    query: Map[JsonOptic, Map[Keyword.Json, Json]] = Map.empty,
    body: XMLString,
    extractors: Map[String, XmlExtractor] = Map.empty,
    inlineCData: Boolean = false
) extends HttpStubRequest {
  override def checkStringBody(bodyStr: String): Boolean =
    extractXML(bodyStr).contains(body.toKNode)

  override def extractJson(bodyStr: String): Option[Json] = None

  override def extractXML(bodyStr: String): Option[Node] =
    if (inlineCData) {
      Try(SafeXML.loadString(bodyStr)).toOption
        .map(_.inlineXmlFromCData.toString)
        .flatMap(XmlSource[String].asNode(_).toOption)
    } else
      XmlSource[String].asNode(bodyStr).toOption

  override def runXmlExtractors(body: Node): Json =
    extractors.view.mapValues(_(body)).collect { case (key, Right(jv)) => key -> jv } pipe Json.fromFields
}

@derive(decoder, encoder)
final case class RawRequest(
    headers: Map[String, String],
    query: Map[JsonOptic, Map[Keyword.Json, Json]] = Map.empty,
    body: String
) extends HttpStubRequest {
  override def checkStringBody(bodyStr: String): Boolean = bodyStr == body

  override def extractJson(bodyStr: String): Option[Json] = None

  override def extractXML(bodyStr: String): Option[Node] = None

  override def runXmlExtractors(body: Node): Json = Json.Null
}

@derive(decoder, encoder)
final case class JLensRequest(
    headers: Map[String, String],
    query: Map[JsonOptic, Map[Keyword.Json, Json]] = Map.empty,
    body: JsonPredicate
) extends HttpStubRequest {
  override def checkStringBody(bodyStr: String): Boolean =
    extractJson(bodyStr).map(body).getOrElse(false)

  override def extractJson(bodyStr: String): Option[Json] =
    parse(bodyStr).toOption

  override def extractXML(bodyStr: String): Option[Node] = None

  override def runXmlExtractors(body: Node): Json = Json.Null
}

@derive(decoder, encoder)
final case class XPathRequest(
    headers: Map[String, String],
    query: Map[JsonOptic, Map[Keyword.Json, Json]] = Map.empty,
    body: XmlPredicate,
    extractors: Map[String, XmlExtractor] = Map.empty,
    inlineCData: Boolean = false
) extends HttpStubRequest {
  override def checkStringBody(bodyStr: String): Boolean =
    extractXML(bodyStr).map(body).getOrElse(false)

  override def extractJson(bodyStr: String): Option[Json] = None

  override def extractXML(bodyStr: String): Option[Node] =
    if (inlineCData) {
      Try(SafeXML.loadString(bodyStr)).toOption
        .map(_.inlineXmlFromCData.toString)
        .flatMap(XmlSource[String].asNode(_).toOption)
    } else
      XmlSource[String].asNode(bodyStr).toOption

  override def runXmlExtractors(body: Node): Json =
    extractors.view.mapValues(_(body)).collect { case (key, Right(jv)) => key -> jv } pipe Json.fromFields
}

@derive(decoder, encoder)
final case class WebFormRequest(
    headers: Map[String, String],
    query: Map[JsonOptic, Map[Keyword.Json, Json]] = Map.empty,
    body: FormPredicate
) extends HttpStubRequest {
  override def checkStringBody(bodyStr: String): Boolean =
    body(decodeForm(bodyStr))

  override def extractJson(bodyStr: String): Option[Json] =
    Some(toJson(decodeForm(bodyStr)))

  override def extractXML(bodyStr: String): Option[Node] = None

  override def runXmlExtractors(body: Node): Json = Json.Null
}

@derive(decoder, encoder)
final case class RequestWithoutBody(
    headers: Map[String, String],
    query: Map[JsonOptic, Map[Keyword.Json, Json]] = Map.empty
) extends HttpStubRequest {
  override def checkStringBody(bodyStr: String): Boolean = bodyStr.isEmpty

  override def extractJson(bodyStr: String): Option[Json] = None

  override def extractXML(bodyStr: String): Option[Node] = None

  override def runXmlExtractors(body: Node): Json = Json.Null
}

@derive(decoder, encoder)
final case class RequestWithAnyBody(
    headers: Map[String, String],
    query: Map[JsonOptic, Map[Keyword.Json, Json]] = Map.empty
) extends HttpStubRequest {
  override def checkStringBody(bodyStr: String): Boolean = bodyStr.nonEmpty

  override def extractJson(bodyStr: String): Option[Json] = None

  override def extractXML(bodyStr: String): Option[Node] = None

  override def runXmlExtractors(body: Node): Json = Json.Null
}
