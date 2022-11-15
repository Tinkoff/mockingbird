package ru.tinkoff.tcb.logging

import tofu.logging.LoggedValue

trait LogContext {
  def mdc(): Mdc

  def correlationId: String = mdc().correlationId match {
    case Some(value) => value
    case _           => java.util.UUID.randomUUID().toString
  }

  def addToPayload(entries: (String, LoggedValue)*): LogContext = LogContext(mdc() ++ entries.toMap)

  def setCorrelationId(value: String): LogContext = LogContext(mdc().setCorrelationId(value))
}

object LogContext {

  val empty: LogContext = () => Mdc.empty

  def apply(mdc: Mdc): LogContext = () => mdc

}
