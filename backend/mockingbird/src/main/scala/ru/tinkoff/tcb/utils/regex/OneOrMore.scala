package ru.tinkoff.tcb.utils.regex

import scala.util.matching.Regex

class OneOrMore(rx: Regex) {
  def unapply(str: String): Boolean = rx.findFirstMatchIn(str).isDefined
}
