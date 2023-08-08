package ru.tinkoff.tcb.mockingbird.dal2.model

import scala.util.matching.Regex

import eu.timepit.refined.types.string.NonEmptyString

/**
 * Потенциально заглушка может сопоставляться с мокируемым путям или как точное соответствие или регулярное выражения,
 * которому удовлетворяет путь на который пришел запрос.
 */
sealed trait StubPath extends Serializable with Product
final case class StubExactlyPath(value: NonEmptyString) extends StubPath
final case class StubPathPattern(value: Regex) extends StubPath
