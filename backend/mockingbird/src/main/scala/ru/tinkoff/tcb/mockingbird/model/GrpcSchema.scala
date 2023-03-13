package ru.tinkoff.tcb.mockingbird.model

import com.github.dwickern.macros.NameOf.nameOfType
import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import enumeratum.CirceEnum
import enumeratum.Enum
import enumeratum.EnumEntry
import enumeratum.EnumEntry.Snakecase
import enumeratum.values.StringCirceEnum
import enumeratum.values.StringEnum
import enumeratum.values.StringEnumEntry
import sttp.tapir.Schema
import sttp.tapir.codec.enumeratum.TapirCodecEnumeratum
import sttp.tapir.derevo.schema
import sttp.tapir.generic.Configuration as TapirConfig

import ru.tinkoff.tcb.bson.annotation.BsonDiscriminator
import ru.tinkoff.tcb.bson.derivation.bsonDecoder
import ru.tinkoff.tcb.bson.derivation.bsonEncoder
import ru.tinkoff.tcb.bson.enumeratum.BsonEnum
import ru.tinkoff.tcb.bson.enumeratum.values.StringBsonValueEnum
import ru.tinkoff.tcb.protocol.schema.*

sealed trait GrpcType extends EnumEntry with Snakecase
object GrpcType extends Enum[GrpcType] with CirceEnum[GrpcType] with BsonEnum[GrpcType] with TapirCodecEnumeratum {
  case object Primitive extends GrpcType
  case object Custom extends GrpcType

  override def values: IndexedSeq[GrpcType] = findValues
}

sealed abstract class GrpcLabel(val value: String) extends StringEnumEntry with Snakecase
object GrpcLabel
    extends StringEnum[GrpcLabel]
    with StringCirceEnum[GrpcLabel]
    with StringBsonValueEnum[GrpcLabel]
    with TapirCodecEnumeratum {
  case object Repeated extends GrpcLabel("repeated")
  case object Required extends GrpcLabel("required")
  case object Optional extends GrpcLabel("optional")

  override def values: IndexedSeq[GrpcLabel] = findValues
}

@derive(encoder, decoder, bsonDecoder, bsonEncoder, schema)
case class GrpcField(
    typ: GrpcType,
    label: GrpcLabel,
    typeName: String,
    name: String,
    order: Int
)

@derive(
  bsonDecoder,
  bsonEncoder,
  decoder(GrpcSchema.modes, true, Some("type")),
  encoder(GrpcSchema.modes, Some("type")),
  schema
)
@BsonDiscriminator("type")
sealed trait GrpcSchema {
  def name: String
}

@derive(
  bsonDecoder,
  bsonEncoder,
  decoder(GrpcRootMessage.modes, true, Some("type")),
  encoder(GrpcRootMessage.modes, Some("type")),
  schema
)
@BsonDiscriminator("type")
sealed trait GrpcRootMessage extends GrpcSchema

object GrpcRootMessage {
  val modes: Map[String, String] = Map(
    nameOfType[GrpcEnumSchema]    -> "enum",
    nameOfType[GrpcMessageSchema] -> "message",
  ).withDefault(identity)

  implicit val customConfiguration: TapirConfig =
    TapirConfig.default.withDiscriminator("type").copy(toEncodedName = modes)
}

object GrpcSchema {
  val modes: Map[String, String] = Map(
    nameOfType[GrpcEnumSchema]    -> "enum",
    nameOfType[GrpcMessageSchema] -> "message",
    nameOfType[GrpcOneOfSchema]   -> "oneof"
  ).withDefault(identity)

  implicit val customConfiguration: TapirConfig =
    TapirConfig.default.withDiscriminator("type").copy(toEncodedName = modes)
}

@derive(encoder, decoder, bsonDecoder, bsonEncoder)
case class GrpcMessageSchema(
    name: String,
    fields: List[GrpcField],
    oneofs: Option[List[GrpcOneOfSchema]] = None,
    nested: Option[List[GrpcMessageSchema]] = None
) extends GrpcRootMessage

object GrpcMessageSchema {
  implicit lazy val gmsSchema: Schema[GrpcMessageSchema] = Schema.derived[GrpcMessageSchema]
}

@derive(encoder, decoder, bsonEncoder, bsonDecoder, schema)
case class GrpcEnumSchema(
    name: String,
    values: Map[FieldName, FieldNumber]
) extends GrpcRootMessage

@derive(encoder, decoder, bsonDecoder, bsonEncoder, schema)
case class GrpcOneOfSchema(
    name: String,
    options: List[GrpcField]
) extends GrpcSchema

@derive(encoder, decoder, bsonDecoder, bsonEncoder, schema)
case class GrpcProtoDefinition(
    name: String,
    schemas: List[GrpcRootMessage],
    `package`: Option[String] = None
)
