package ru.tinkoff.tcb.mockingbird.error

case class CompoundError(excs: List[Throwable]) extends Exception
