package ru.tinkoff.tcb.utils.transformation

import io.circe.Json
import kantan.xpath.Node

import ru.tinkoff.tcb.utils.transformation.json.*

package object string {
  implicit final class StringTransformations(private val s: String) extends AnyVal {
    def substitute(jvalues: Json, xvalues: Node): String =
      if (s.contains("${") && s.contains("}"))
        Json.fromString(s).substitute(jvalues).substitute(xvalues).asString.getOrElse(s)
      else s

    def substitute(values: Json): String =
      if (s.contains("${") && s.contains("}"))
        Json.fromString(s).substitute(values).asString.getOrElse(s)
      else s
  }
}
