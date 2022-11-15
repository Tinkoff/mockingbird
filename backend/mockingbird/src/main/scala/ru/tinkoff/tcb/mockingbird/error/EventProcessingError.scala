package ru.tinkoff.tcb.mockingbird.error

final case class EventProcessingError(message: String) extends Exception(message)
