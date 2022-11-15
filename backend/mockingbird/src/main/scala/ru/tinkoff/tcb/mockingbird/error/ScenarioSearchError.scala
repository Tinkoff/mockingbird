package ru.tinkoff.tcb.mockingbird.error

final case class ScenarioSearchError(message: String) extends Exception(message)
