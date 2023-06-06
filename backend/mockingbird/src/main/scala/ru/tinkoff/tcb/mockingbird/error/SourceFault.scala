package ru.tinkoff.tcb.mockingbird.error

final case class SourceFault(message: String) extends Exception(message)
