package ru.tinkoff.tcb.mockingbird

import java.nio.charset.StandardCharsets

import sttp.tapir.Codec
import sttp.tapir.CodecFormat
import sttp.tapir.DecodeResult
import sttp.tapir.Schema
import sttp.tapir.SchemaType

import ru.tinkoff.tcb.mockingbird.model.BinaryResponse
import ru.tinkoff.tcb.mockingbird.model.HttpStubResponse
import ru.tinkoff.tcb.mockingbird.model.JsonProxyResponse
import ru.tinkoff.tcb.mockingbird.model.JsonResponse
import ru.tinkoff.tcb.mockingbird.model.ProxyResponse
import ru.tinkoff.tcb.mockingbird.model.RawResponse
import ru.tinkoff.tcb.mockingbird.model.XmlProxyResponse
import ru.tinkoff.tcb.mockingbird.model.XmlResponse

package object codec {
  implicit val throwableCodec: Codec[String, Throwable, CodecFormat.TextPlain] =
    Codec.string.map[Throwable](new Exception(_: String))(_.toString)

  implicit val httpStubResponseCodec: Codec[Array[Byte], HttpStubResponse, CodecFormat.OctetStream] =
    new Codec[Array[Byte], HttpStubResponse, CodecFormat.OctetStream] {
      override def rawDecode(l: Array[Byte]): DecodeResult[HttpStubResponse] =
        throw new UnsupportedOperationException()

      override def schema: Schema[HttpStubResponse] = Schema(SchemaType.SProduct[HttpStubResponse](Nil))

      override def format: CodecFormat.OctetStream = CodecFormat.OctetStream()

      override def encode(h: HttpStubResponse): Array[Byte] = h match {
        case RawResponse(_, _, body, _)     => body.getBytes(StandardCharsets.UTF_8)
        case JsonResponse(_, _, body, _, _) => body.noSpaces.getBytes(StandardCharsets.UTF_8)
        case XmlResponse(_, _, body, _, _)  => body.asString.getBytes(StandardCharsets.UTF_8)
        case BinaryResponse(_, _, body, _)  => body.asArray
        /**
         * все *ProxyResponse преобразуются в RawResponse внутри [[PublicApiHandler]] (методы *proxyRequest)
         */
        case ProxyResponse(_, _, _)        => throw new UnsupportedOperationException()
        case JsonProxyResponse(_, _, _, _) => throw new UnsupportedOperationException()
        case XmlProxyResponse(_, _, _, _)  => throw new UnsupportedOperationException()
      }
    }

  implicit val binaryOptionalStringCodec: Codec[Array[Byte], Option[String], CodecFormat.OctetStream] =
    Codec.byteArray.map(arr => if (arr.nonEmpty) Some(new String(arr, StandardCharsets.UTF_8)) else None)(
      _.fold(Array.emptyByteArray)(_.getBytes(StandardCharsets.UTF_8))
    )
}
