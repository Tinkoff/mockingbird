package ru.tinkoff.tcb.utils.sandboxing

import java.util.Map as JMap

import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.WrapFactory
import org.ringojs.wrappers.ScriptableMap

object SandboxWrapFactory extends WrapFactory {
  override def wrapAsJavaObject(
      cx: Context,
      scope: Scriptable,
      javaObject: AnyRef,
      staticType: Class[?]
  ): Scriptable =
    javaObject.getClass match {
      case jmap if classOf[JMap[?, ?]].isAssignableFrom(jmap) =>
        new ScriptableMap(scope, javaObject.asInstanceOf[JMap[?, ?]])
      case _ => super.wrapAsJavaObject(cx, scope, javaObject, staticType)
    }
}
