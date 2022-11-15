package ru.tinkoff.tcb.mockingbird

import io.circe.literal.*

package object api {
  /*
    "World" for this application
   */
  type WLD = Tracing

  def mkErrorResponse(message: String): String =
    json"""{"error": $message}""".noSpaces
}
