package ru.tinkoff.tcb.mockingbird.model

import scala.xml.Node

import com.github.dwickern.macros.NameOf.nameOfType
import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import io.circe.Json
import io.circe.refined.*
import sttp.tapir.codec.refined.*
import sttp.tapir.derevo.schema
import sttp.tapir.generic.Configuration as TapirConfig

import ru.tinkoff.tcb.bson.annotation.BsonDiscriminator
import ru.tinkoff.tcb.bson.derivation.bsonDecoder
import ru.tinkoff.tcb.bson.derivation.bsonEncoder
import ru.tinkoff.tcb.circe.bson.*
import ru.tinkoff.tcb.protocol.bson.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.xml.XMLString

@derive(
  bsonDecoder,
  bsonEncoder,
  decoder(CallbackRequest.modes, true, Some("mode")),
  encoder(CallbackRequest.modes, Some("mode")),
  schema
)
@BsonDiscriminator("mode")
sealed trait CallbackRequest {
  def url: String Refined NonEmpty
  def method: HttpMethod
  def headers: Map[String, String]
}

object CallbackRequest {
  val modes: Map[String, String] = Map(
    nameOfType[CallbackRequestWithoutBody] -> "no_body",
    nameOfType[RawCallbackRequest]         -> "raw",
    nameOfType[JsonCallbackRequest]        -> "json",
    nameOfType[XMLCallbackRequest]         -> "xml"
  ).withDefault(identity)

  implicit val customConfiguration: TapirConfig =
    TapirConfig.default.withDiscriminator("mode").copy(toEncodedName = modes)
}

@derive(decoder, encoder)
case class CallbackRequestWithoutBody(
    url: String Refined NonEmpty,
    method: HttpMethod,
    headers: Map[String, String]
) extends CallbackRequest

@derive(decoder, encoder)
case class RawCallbackRequest(
    url: String Refined NonEmpty,
    method: HttpMethod,
    headers: Map[String, String],
    body: String
) extends CallbackRequest

@derive(decoder, encoder)
case class JsonCallbackRequest(
    url: String Refined NonEmpty,
    method: HttpMethod,
    headers: Map[String, String],
    body: Json
) extends CallbackRequest

@derive(decoder, encoder)
case class XMLCallbackRequest(
    url: String Refined NonEmpty,
    method: HttpMethod,
    headers: Map[String, String],
    body: XMLString
) extends CallbackRequest {
  lazy val node: Node = body.toNode
}
