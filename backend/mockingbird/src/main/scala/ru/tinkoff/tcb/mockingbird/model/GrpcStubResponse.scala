package ru.tinkoff.tcb.mockingbird.model

import scala.concurrent.duration.FiniteDuration

import com.github.dwickern.macros.NameOf.*
import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import io.circe.Json
import sttp.tapir.derevo.schema
import sttp.tapir.generic.Configuration as TapirConfig

import ru.tinkoff.tcb.bson.annotation.BsonDiscriminator
import ru.tinkoff.tcb.bson.derivation.bsonDecoder
import ru.tinkoff.tcb.bson.derivation.bsonEncoder
import ru.tinkoff.tcb.circe.bson.*
import ru.tinkoff.tcb.protocol.bson.*
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic

@derive(
  bsonDecoder,
  bsonEncoder,
  decoder(GrpcStubResponse.modes, true, Some("mode")),
  encoder(GrpcStubResponse.modes, Some("mode")),
  schema
)
@BsonDiscriminator("mode")
sealed trait GrpcStubResponse {
  def delay: Option[FiniteDuration]
}

object GrpcStubResponse {
  val modes: Map[String, String] = Map(
    nameOfType[FillResponse]   -> "fill",
    nameOfType[GProxyResponse] -> "proxy"
  ).withDefault(identity)

  implicit val customConfiguration: TapirConfig =
    TapirConfig.default.withDiscriminator("mode").copy(toEncodedName = modes)
}

@derive(decoder, encoder)
final case class FillResponse(
    data: Json,
    delay: Option[FiniteDuration]
) extends GrpcStubResponse

@derive(decoder, encoder)
final case class GProxyResponse(
    endpoint: String,
    patch: Map[JsonOptic, String],
    delay: Option[FiniteDuration]
) extends GrpcStubResponse
