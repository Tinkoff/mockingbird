package ru.tinkoff.tcb.mockingbird.error

final case class ResourceManagementError(message: String) extends Exception(message)
