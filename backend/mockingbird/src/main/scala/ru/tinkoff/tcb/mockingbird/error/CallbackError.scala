package ru.tinkoff.tcb.mockingbird.error

final case class CallbackError(message: String) extends Exception(message)
