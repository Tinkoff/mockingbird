package ru.tinkoff.tcb.utils

import scala.util.matching.Regex

package object regex {
  private val Group = "<(?<name>[a-zA-Z0-9]+)>".r

  implicit class RegexExt(private val rx: Regex) {
    def groups: Seq[String] = Group.findAllMatchIn(rx.pattern.pattern()).map(_.group("name")).to(Seq)
  }
}
