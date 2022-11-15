package ru.tinkoff.tcb.protocol

import io.circe.Json

import ru.tinkoff.tcb.generic.RootOptionFields

object rof {
  implicit val jsonRof: RootOptionFields[Json] = RootOptionFields.mk(Set.empty)
}
