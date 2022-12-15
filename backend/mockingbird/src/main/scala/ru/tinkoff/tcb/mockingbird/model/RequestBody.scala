package ru.tinkoff.tcb.mockingbird.model

import derevo.derive
import glass.Subset
import glass.macros.GenSubset
import sttp.model.Part
import tofu.logging.Loggable
import tofu.logging.derivation.loggable

import ru.tinkoff.tcb.protocol.log.*

@derive(loggable)
sealed trait RequestBody
object RequestBody

case object AbsentRequestBody extends RequestBody {
  final val subset: Subset[RequestBody, AbsentRequestBody.type] = GenSubset[RequestBody, AbsentRequestBody.type]

  implicit val absentRequestBodyLoggable: Loggable[AbsentRequestBody.type] = Loggable.empty[AbsentRequestBody.type]
}

final case class SimpleRequestBody(value: String) extends RequestBody
object SimpleRequestBody {
  final val subset: Subset[RequestBody, SimpleRequestBody] = GenSubset[RequestBody, SimpleRequestBody]

  implicit val simpleRequestBodyLoggable: Loggable[SimpleRequestBody] =
    Loggable.stringValue.contramap[SimpleRequestBody](_.value)
}

@derive(loggable)
final case class MultipartRequestBody(value: Seq[Part[String]]) extends RequestBody
object MultipartRequestBody {
  final val subset: Subset[RequestBody, MultipartRequestBody] = GenSubset[RequestBody, MultipartRequestBody]
}
