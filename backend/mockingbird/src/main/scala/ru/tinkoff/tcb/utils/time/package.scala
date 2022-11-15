package ru.tinkoff.tcb.utils

import java.time.format.DateTimeFormatter
import scala.util.Try

package object time {
  object Formatter {
    def unapply(arg: String): Option[DateTimeFormatter] =
      Try(DateTimeFormatter.ofPattern(arg)).toOption
  }
}
