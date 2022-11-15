package ru.tinkoff.tcb.mockingbird.error

final case class DuplicationError(message: String, ids: Vector[String]) extends Exception(message)
