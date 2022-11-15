package ru.tinkoff.tcb.mockingbird.model

import scala.concurrent.duration.FiniteDuration

import com.github.dwickern.macros.NameOf.*
import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import io.circe.Json
import sttp.tapir.Schema
import sttp.tapir.generic.Configuration as TapirConfig

import ru.tinkoff.tcb.bson.annotation.BsonDiscriminator
import ru.tinkoff.tcb.bson.derivation.bsonDecoder
import ru.tinkoff.tcb.bson.derivation.bsonEncoder
import ru.tinkoff.tcb.circe.bson.*
import ru.tinkoff.tcb.protocol.bson.*
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.utils.id.SID

@derive(
  bsonDecoder,
  bsonEncoder,
  decoder(Callback.modes, true, Some("type")),
  encoder(Callback.modes, Some("type"))
)
@BsonDiscriminator("type")
sealed trait Callback {
  def delay: Option[FiniteDuration]
}

object Callback {
  val modes: Map[String, String] = Map(
    nameOfType[MessageCallback] -> "message",
    nameOfType[HttpCallback]    -> "http"
  ).withDefault(identity)

  implicit val customConfiguration: TapirConfig =
    TapirConfig.default.withDiscriminator("mode").copy(toEncodedName = modes)

  implicit lazy val callbackSchema: Schema[Callback] = Schema.derived[Callback]
}

@derive(decoder, encoder)
case class MessageCallback(
    destination: SID[DestinationConfiguration],
    output: ScenarioOutput,
    callback: Option[Callback],
    delay: Option[FiniteDuration] = None
) extends Callback

@derive(decoder, encoder)
case class HttpCallback(
    request: CallbackRequest,
    responseMode: Option[CallbackResponseMode],
    persist: Option[Map[JsonOptic, Json]],
    callback: Option[Callback],
    delay: Option[FiniteDuration] = None
) extends Callback
