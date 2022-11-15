package ru.tinkoff.tcb.protocol

import io.circe.Json
import io.circe.parser.parse
import kantan.codecs.strings.DecodeError
import kantan.codecs.strings.StringDecoder

object xml {
  implicit val jsonNodeDecoder: StringDecoder[Json] =
    StringDecoder.from(s => parse(s.trim()).leftMap(pf => DecodeError(pf.message)))
}
