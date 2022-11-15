package ru.tinkoff.tcb.mockingbird.error

final case class StubSearchError(message: String) extends Exception(message)
