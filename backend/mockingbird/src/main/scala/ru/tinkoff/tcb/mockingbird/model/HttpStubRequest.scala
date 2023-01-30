package ru.tinkoff.tcb.mockingbird.model

import scala.util.Try

import com.github.dwickern.macros.NameOf.*
import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import io.circe.Json
import io.circe.JsonObject
import io.circe.parser.parse
import kantan.xpath.Node
import kantan.xpath.XmlSource
import sttp.model.Part
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

  def checkBody(rBody: RequestBody): Boolean

  def extractJson(rBody: RequestBody): Option[Json]

  def extractXML(rBody: RequestBody): Option[Node]

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
    nameOfType[WebFormRequest]     -> "web_form",
    nameOfType[MultipartRequest]   -> "multipart"
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
  override def checkBody(rBody: RequestBody): Boolean =
    extractJson(rBody).contains(body)

  override def extractJson(rBody: RequestBody): Option[Json] =
    SimpleRequestBody.subset.getOption(rBody).map(_.value).flatMap(parse(_).toOption)

  override def extractXML(rBody: RequestBody): Option[Node] = None

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
  override def checkBody(rBody: RequestBody): Boolean =
    extractXML(rBody).contains(body.toKNode)

  override def extractJson(rBody: RequestBody): Option[Json] = None

  override def extractXML(rBody: RequestBody): Option[Node] =
    SimpleRequestBody.subset.getOption(rBody).map(_.value).flatMap { bodyStr =>
      if (inlineCData) {
        Try(SafeXML.loadString(bodyStr)).toOption
          .map(_.inlineXmlFromCData.toString)
          .flatMap(XmlSource[String].asNode(_).toOption)
      } else
        XmlSource[String].asNode(bodyStr).toOption
    }

  override def runXmlExtractors(body: Node): Json =
    extractors.view.mapValues(_(body)).collect { case (key, Right(jv)) => key -> jv } pipe Json.fromFields
}

@derive(decoder, encoder)
final case class RawRequest(
    headers: Map[String, String],
    query: Map[JsonOptic, Map[Keyword.Json, Json]] = Map.empty,
    body: String
) extends HttpStubRequest {
  override def checkBody(rBody: RequestBody): Boolean =
    SimpleRequestBody.subset.getOption(rBody).map(_.value).contains(body)

  override def extractJson(rBody: RequestBody): Option[Json] = None

  override def extractXML(rBody: RequestBody): Option[Node] = None

  override def runXmlExtractors(body: Node): Json = Json.Null
}

@derive(decoder, encoder)
final case class JLensRequest(
    headers: Map[String, String],
    query: Map[JsonOptic, Map[Keyword.Json, Json]] = Map.empty,
    body: JsonPredicate
) extends HttpStubRequest {
  override def checkBody(rBody: RequestBody): Boolean =
    extractJson(rBody).map(body).getOrElse(false)

  override def extractJson(rBody: RequestBody): Option[Json] =
    SimpleRequestBody.subset.getOption(rBody).map(_.value).flatMap(parse(_).toOption)

  override def extractXML(rBody: RequestBody): Option[Node] = None

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
  override def checkBody(rBody: RequestBody): Boolean =
    extractXML(rBody).map(body).getOrElse(false)

  override def extractJson(rBody: RequestBody): Option[Json] = None

  override def extractXML(rBody: RequestBody): Option[Node] =
    SimpleRequestBody.subset.getOption(rBody).map(_.value).flatMap { bodyStr =>
      if (inlineCData) {
        Try(SafeXML.loadString(bodyStr)).toOption
          .map(_.inlineXmlFromCData.toString)
          .flatMap(XmlSource[String].asNode(_).toOption)
      } else
        XmlSource[String].asNode(bodyStr).toOption
    }

  override def runXmlExtractors(body: Node): Json =
    extractors.view.mapValues(_(body)).collect { case (key, Right(jv)) => key -> jv } pipe Json.fromFields
}

@derive(decoder, encoder)
final case class WebFormRequest(
    headers: Map[String, String],
    query: Map[JsonOptic, Map[Keyword.Json, Json]] = Map.empty,
    body: FormPredicate
) extends HttpStubRequest {
  private def extractForm(rBody: RequestBody) =
    SimpleRequestBody.subset.getOption(rBody).map(_.value).map(decodeForm)

  override def checkBody(rBody: RequestBody): Boolean =
    extractForm(rBody).map(body).getOrElse(false)

  override def extractJson(rBody: RequestBody): Option[Json] =
    extractForm(rBody).map(toJson)

  override def extractXML(rBody: RequestBody): Option[Node] = None

  override def runXmlExtractors(body: Node): Json = Json.Null
}

@derive(decoder, encoder)
final case class RequestWithoutBody(
    headers: Map[String, String],
    query: Map[JsonOptic, Map[Keyword.Json, Json]] = Map.empty
) extends HttpStubRequest {
  override def checkBody(rBody: RequestBody): Boolean =
    AbsentRequestBody.subset.getOption(rBody).isDefined ||
      SimpleRequestBody.subset.getOption(rBody).exists(_.value.isEmpty)

  override def extractJson(rBody: RequestBody): Option[Json] = None

  override def extractXML(rBody: RequestBody): Option[Node] = None

  override def runXmlExtractors(body: Node): Json = Json.Null
}

@derive(decoder, encoder)
final case class RequestWithAnyBody(
    headers: Map[String, String],
    query: Map[JsonOptic, Map[Keyword.Json, Json]] = Map.empty
) extends HttpStubRequest {
  override def checkBody(rBody: RequestBody): Boolean =
    SimpleRequestBody.subset.getOption(rBody).exists(_.value.nonEmpty)

  override def extractJson(rBody: RequestBody): Option[Json] = None

  override def extractXML(rBody: RequestBody): Option[Node] = None

  override def runXmlExtractors(body: Node): Json = Json.Null
}

@derive(
  bsonDecoder,
  bsonEncoder,
  decoder(RequestPart.modes, true, Some("mode")),
  encoder(RequestPart.modes, Some("mode")),
  schema
)
@BsonDiscriminator("mode")
sealed trait RequestPart {
  def headers: Map[String, String]

  def checkHeaders(hs: Map[String, String]): Boolean =
    headers.forall { case (k, v) =>
      hs.exists {
        case (kx, vx) => k.toLowerCase == kx.toLowerCase && v == vx
        case _        => false
      }
    }

  def checkPart(part: Part[String]): Boolean =
    checkHeaders(part.headers.map(h => h.name -> h.value).toMap) && checkBody(part.body)

  def checkBody(value: String): Boolean

  def extractJson(body: String): Option[Json]

  def extractXML(body: String): Option[Node]
}
object RequestPart {
  val modes: Map[String, String] = Map(
    nameOfType[AnyContentPart] -> "any",
    nameOfType[RawPart]        -> "raw",
    nameOfType[JsonPart]       -> "json",
    nameOfType[XMLPart]        -> "xml",
    nameOfType[UrlEncodedPart] -> "urlencoded",
    nameOfType[JLensPart]      -> "jlens",
    nameOfType[XPathPart]      -> "xpath"
  ).withDefault(identity)

  implicit val customConfiguration: TapirConfig =
    TapirConfig.default.withDiscriminator("mode").copy(toEncodedName = modes)
}

@derive(decoder, encoder)
final case class AnyContentPart(headers: Map[String, String]) extends RequestPart {
  override def checkBody(value: String): Boolean = true

  override def extractJson(body: String): Option[Json] = None

  override def extractXML(body: String): Option[Node] = None
}

@derive(decoder, encoder)
final case class RawPart(headers: Map[String, String], body: String) extends RequestPart {
  override def checkBody(value: String): Boolean = value == body

  override def extractJson(body: String): Option[Json] = None

  override def extractXML(body: String): Option[Node] = None
}

@derive(decoder, encoder)
final case class UrlEncodedPart(headers: Map[String, String], body: FormPredicate) extends RequestPart {
  override def checkBody(value: String): Boolean = body(decodeForm(value))

  override def extractJson(body: String): Option[Json] = Option(toJson(decodeForm(body)))

  override def extractXML(body: String): Option[Node] = None
}

@derive(decoder, encoder)
final case class JsonPart(headers: Map[String, String], body: Json) extends RequestPart {
  override def checkBody(value: String): Boolean = parse(value).contains(body)

  override def extractJson(body: String): Option[Json] = parse(body).toOption

  override def extractXML(body: String): Option[Node] = None
}

@derive(decoder, encoder)
final case class XMLPart(headers: Map[String, String], body: XMLString) extends RequestPart {
  override def checkBody(value: String): Boolean = XmlSource[String].asNode(value).contains(body.toKNode)

  override def extractJson(body: String): Option[Json] = None

  override def extractXML(body: String): Option[Node] = XmlSource[String].asNode(body).toOption
}

@derive(decoder, encoder)
final case class JLensPart(headers: Map[String, String], body: JsonPredicate) extends RequestPart {
  override def checkBody(value: String): Boolean = parse(value).map(body).getOrElse(false)

  override def extractJson(body: String): Option[Json] = parse(body).toOption

  override def extractXML(body: String): Option[Node] = None
}

@derive(decoder, encoder)
final case class XPathPart(headers: Map[String, String], body: XmlPredicate) extends RequestPart {
  override def checkBody(value: String): Boolean = XmlSource[String].asNode(value).map(body).getOrElse(false)

  override def extractJson(body: String): Option[Json] = None

  override def extractXML(body: String): Option[Node] = XmlSource[String].asNode(body).toOption
}

@derive(decoder, encoder)
final case class MultipartRequest(
    headers: Map[String, String],
    query: Map[JsonOptic, Map[Keyword.Json, Json]] = Map.empty,
    body: Map[String, RequestPart],
    bypassUnknownParts: Boolean = true
) extends HttpStubRequest {
  override def checkBody(rBody: RequestBody): Boolean =
    MultipartRequestBody.subset.getOption(rBody).exists { requestBody =>
      requestBody.value.forall { requestBodyPart =>
        (!body.keySet.contains(requestBodyPart.name) && bypassUnknownParts) || body(requestBodyPart.name)
          .checkPart(requestBodyPart)
      }
    }

  override def extractJson(rBody: RequestBody): Option[Json] =
    MultipartRequestBody.subset.getOption(rBody).flatMap { requestBody =>
      val jo = requestBody.value
        .map(p => body.get(p.name).flatMap(rp => rp.extractJson(p.body)).map(p.name -> _))
        .collect { case Some(kv) => kv }
        .foldLeft(JsonObject.empty) { case (acc, (key, value)) => acc.add(key, value) }
      if (jo.isEmpty) None else Some(Json.fromJsonObject(jo))
    }

  override def extractXML(rBody: RequestBody): Option[Node] =
    MultipartRequestBody.subset.getOption(rBody).flatMap { requestBody =>
      requestBody.value.view.map(p => body.get(p.name).flatMap(rp => rp.extractXML(p.body))).collectFirst {
        case Some(node) => node
      }
    }

  override def runXmlExtractors(body: Node): Json = Json.Null
}
