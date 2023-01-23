package ru.tinkoff.tcb.mockingbird.error

import ru.tinkoff.tcb.utils.id.SID

final case class SpawnError(source: SID[?], cause: Throwable) extends Exception(cause)
