package ru.tinkoff.tcb.mockingbird.api.response

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import sttp.tapir.derevo.schema

@derive(decoder, encoder, schema)
final case class OperationResult[T](status: String, id: Option[T] = None)

object OperationResult {
  def apply[T](status: String, id: T): OperationResult[T] = OperationResult(status, Some(id))
}
