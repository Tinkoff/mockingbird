package ru.tinkoff.tcb.utils.sandboxing

import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag
import scala.reflect.classTag

import org.mozilla.javascript.Context

class RhinoJsSandbox(maxRunTime: Option[FiniteDuration] = None, maxInstructions: Option[Long] = None) {
  private val contextFactory = new SafeContextFactory(maxRunTime, maxInstructions)

  def eval[T: ClassTag](code: String, environment: Map[String, Any] = Map.empty): T = {
    val ctx = contextFactory.enterContext()

    try {
      val scope = ctx.initSafeStandardObjects()
      for ((key, value) <- environment)
        scope.put(key, scope, Context.javaToJS(value, scope, ctx))

      val result = ctx.evaluateString(scope, code, "virtual", 1, null)

      Context.jsToJava(result, classTag[T].runtimeClass).asInstanceOf[T]
    } finally
      Context.exit()
  }
}
