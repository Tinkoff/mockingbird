package ru.tinkoff.tcb.mockingbird.model

import scala.concurrent.duration.FiniteDuration

import com.github.dwickern.macros.NameOf.*
import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import sttp.tapir.derevo.schema
import sttp.tapir.generic.Configuration as TapirConfig

import ru.tinkoff.tcb.bson.annotation.BsonDiscriminator
import ru.tinkoff.tcb.bson.derivation.bsonDecoder
import ru.tinkoff.tcb.bson.derivation.bsonEncoder
import ru.tinkoff.tcb.circe.bson.*
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.transformation.json.JsonTransformations
import ru.tinkoff.tcb.utils.transformation.xml.XmlTransformation
import ru.tinkoff.tcb.utils.xml.XMLString

@derive(
  bsonDecoder,
  bsonEncoder,
  decoder(ScenarioOutput.modes, true, Some("mode")),
  encoder(ScenarioOutput.modes, Some("mode")),
  schema
)
@BsonDiscriminator("mode")
sealed trait ScenarioOutput {
  def delay: Option[FiniteDuration]
  def isTemplate: Boolean
}

object ScenarioOutput {
  val modes: Map[String, String] = Map(
    nameOfType[RawOutput]  -> "raw",
    nameOfType[JsonOutput] -> "json",
    nameOfType[XmlOutput]  -> "xml"
  ).withDefault(identity)

  implicit val customConfiguration: TapirConfig =
    TapirConfig.default.withDiscriminator("mode").copy(toEncodedName = modes)
}

@derive(decoder, encoder)
final case class RawOutput(
    payload: String,
    delay: Option[FiniteDuration]
) extends ScenarioOutput {
  val isTemplate = false
}

final case class JsonOutput(
    payload: Json,
    delay: Option[FiniteDuration],
    isTemplate: Boolean = true
) extends ScenarioOutput

object JsonOutput {
  implicit val joEncoder: Encoder.AsObject[JsonOutput] =
    Encoder.forProduct2(
      nameOf[JsonOutput](_.payload),
      nameOf[JsonOutput](_.delay)
    )(jo => (jo.payload, jo.delay))

  implicit val joDecoder: Decoder[JsonOutput] =
    Decoder.forProduct2(nameOf[JsonOutput](_.payload), nameOf[JsonOutput](_.delay))((pl, dl) =>
      JsonOutput(pl, dl, pl.isTemplate)
    )
}

final case class XmlOutput(
    payload: XMLString,
    delay: Option[FiniteDuration],
    isTemplate: Boolean = true
) extends ScenarioOutput

object XmlOutput {
  implicit val xoEncoder: Encoder.AsObject[XmlOutput] =
    Encoder.forProduct2(
      nameOf[XmlOutput](_.payload),
      nameOf[XmlOutput](_.delay)
    )(jo => (jo.payload, jo.delay))

  implicit val xoDecoder: Decoder[XmlOutput] =
    Decoder.forProduct2(nameOf[XmlOutput](_.payload), nameOf[XmlOutput](_.delay))((pl, dl) =>
      XmlOutput(pl, dl, pl.toNode.isTemplate)
    )
}
