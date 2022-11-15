package ru.tinkoff.tcb.mockingbird.error

final case class ValidationError(fails: Vector[String]) extends Exception(s"Ошибки валидации: ${fails.mkString(",")}")
