package ru.tinkoff.tcb.utils.sandboxing

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

import org.mozilla.javascript.Callable
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.Scriptable

class SafeContextFactory(maxRunTime: Option[FiniteDuration], maxInstructions: Option[Long]) extends ContextFactory {
  override def makeContext(): Context =
    new CounterContext(this).tap { cc =>
      cc.setLanguageVersion(Context.VERSION_ES6)
      cc.setOptimizationLevel(-1)
      cc.setInstructionObserverThreshold(CounterContext.InstructionSteps)
      cc.setClassShutter(SafeClassShutter)
    }

  override def hasFeature(cx: Context, featureIndex: RuntimeFlags): Boolean = featureIndex match {
    case Context.FEATURE_NON_ECMA_GET_YEAR | Context.FEATURE_MEMBER_EXPR_AS_FUNCTION_NAME |
        Context.FEATURE_RESERVED_KEYWORD_AS_IDENTIFIER =>
      true
    case Context.FEATURE_PARENT_PROTO_PROPERTIES => false
    case _                                       => super.hasFeature(cx, featureIndex)
  }

  override def observeInstructionCount(cx: Context, instructionCount: RuntimeFlags): Unit = {
    val counter = cx.asInstanceOf[CounterContext]

    if (counter.deadline.isOverdue())
      throw new ScriptDurationException

    counter.instructions += CounterContext.InstructionSteps
    if (maxInstructions.exists(_ <= counter.instructions))
      throw new ScriptCPUAbuseException
  }

  override def doTopCall(
      callable: Callable,
      cx: Context,
      scope: Scriptable,
      thisObj: Scriptable,
      args: Array[AnyRef]
  ): AnyRef = {
    val counter = cx.asInstanceOf[CounterContext]
    counter.deadline = maxRunTime.fold(minute.fromNow)(fd => fd.fromNow)
    counter.instructions = 0
    super.doTopCall(callable, cx, scope, thisObj, args)
  }

  final private val minute = FiniteDuration(1, TimeUnit.MINUTES)
}
