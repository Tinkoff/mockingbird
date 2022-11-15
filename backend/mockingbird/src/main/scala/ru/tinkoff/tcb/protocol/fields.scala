package ru.tinkoff.tcb.protocol

import ru.tinkoff.tcb.generic.Fields
import ru.tinkoff.tcb.utils.id.SID

object fields {
  implicit def sidFields[T]: Fields[SID[T]] = Fields.mk(Nil)
}
