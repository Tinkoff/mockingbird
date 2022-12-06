package ru.tinkoff.tcb.utils.sandboxing

import scala.concurrent.duration.Deadline

import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory

class CounterContext(factory: ContextFactory) extends Context(factory) {
  var deadline: Deadline = Deadline.now
  var instructions: Long = 0
}

object CounterContext {
  final val InstructionSteps = 10000
}
