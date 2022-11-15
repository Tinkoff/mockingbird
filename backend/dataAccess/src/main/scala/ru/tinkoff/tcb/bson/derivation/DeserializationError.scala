package ru.tinkoff.tcb.bson.derivation

case class DeserializationError(message: String, cause: Throwable) extends RuntimeException(message, cause) {
  def this(message: String) = this(message, null)
}
object DeserializationError {
  def apply(message: String): DeserializationError =
    new DeserializationError(message)
}
