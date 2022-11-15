package ru.tinkoff.tcb.mockingbird.error

final case class ScenarioExecError(message: String) extends Exception(message)
