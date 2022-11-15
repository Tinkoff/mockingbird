package ru.tinkoff.tcb.mockingbird

import java.util.Base64
import scala.util.Try

import io.circe.Decoder
import io.circe.Encoder
import io.circe.KeyDecoder
import io.circe.KeyEncoder
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops.*
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.BsonString
import sttp.tapir.Schema

import ru.tinkoff.tcb.bson.BsonDecoder
import ru.tinkoff.tcb.bson.BsonEncoder
import ru.tinkoff.tcb.bson.BsonKeyDecoder
import ru.tinkoff.tcb.bson.BsonKeyEncoder
import ru.tinkoff.tcb.generic.RootOptionFields
import ru.tinkoff.tcb.utils.crypto.AES

package object model {
  @newtype class ByteArray private (val asArray: Array[Byte])

  object ByteArray {
    implicit val byteArrayBsonEncoder: BsonEncoder[ByteArray] =
      BsonEncoder[Array[Byte]].coerce

    implicit val byteArrayBsonDecoder: BsonDecoder[ByteArray] =
      BsonDecoder[Array[Byte]].coerce

    implicit val byteArraySchema: Schema[ByteArray] =
      Schema.schemaForString.as[ByteArray].format("base64")

    implicit val byteArrayDecoder: Decoder[ByteArray] =
      Decoder.decodeString.emapTry(s => Try(Base64.getDecoder.decode(s))).map(_.coerce)

    implicit val byteArrayEncoder: Encoder[ByteArray] =
      Encoder.encodeString.contramap(ba => Base64.getEncoder.encodeToString(ba.asArray))

    implicit val byteArrayRof: RootOptionFields[ByteArray] = RootOptionFields.mk(Set.empty)
  }

  @newtype class FieldNumber private (val asInt: Int)

  object FieldNumber {
    implicit val fieldNumberSchema: Schema[FieldNumber] =
      Schema.schemaForInt.as[FieldNumber]

    implicit val fieldNumberEncoder: Encoder[FieldNumber] =
      Encoder[Int].coerce

    implicit val fieldNumberDecoer: Decoder[FieldNumber] =
      Decoder[Int].coerce

    implicit val fieldNumberBsonEncoder: BsonEncoder[FieldNumber] =
      BsonEncoder[Int].coerce

    implicit val fieldNumberBsonDecoder: BsonDecoder[FieldNumber] =
      BsonDecoder[Int].coerce
  }

  @newtype class FieldName private (val asString: String)

  object FieldName {
    implicit val fieldNameKeyEncoder: KeyEncoder[FieldName] =
      KeyEncoder[String].coerce

    implicit val fieldNameKeyDecoder: KeyDecoder[FieldName] =
      KeyDecoder[String].coerce

    implicit val fieldNameBsonKeyEncoder: BsonKeyEncoder[FieldName] =
      (t: FieldName) => t.asString

    implicit val fieldNameBsonKeyDecoder: BsonKeyDecoder[FieldName] =
      (value: String) => Try(value.coerce[FieldName])
  }

  @newtype class SecureString private (val asString: String)

  object SecureString {
    implicit def secureStringBsonEncoder(implicit aes: AES): BsonEncoder[SecureString] =
      (value: SecureString) => {
        val (data, salt, iv) = aes.encrypt(value.asString)

        BsonDocument(
          "d" -> BsonString(data),
          "s" -> BsonString(salt),
          "i" -> BsonString(iv)
        )
      }

    implicit def secureStringBsonDecoder(implicit aes: AES): BsonDecoder[SecureString] =
      BsonDecoder.ofDocument[SecureString] { doc =>
        (
          Try(doc.getString("d")),
          Try(doc.getString("s")),
          Try(doc.getString("i"))
        ).mapN { case (data, salt, iv) => aes.decrypt(data.getValue, salt.getValue, iv.getValue).coerce }
      }

    implicit val secureStringSchema: Schema[SecureString] =
      Schema.schemaForString.as[SecureString]

    implicit val secureStringDecoder: Decoder[SecureString] =
      Decoder.decodeString.coerce

    implicit val secureStringEncoder: Encoder[SecureString] =
      Encoder.encodeString.coerce

    implicit val secureStringRof: RootOptionFields[SecureString] = RootOptionFields.mk(Set.empty)
  }
}
