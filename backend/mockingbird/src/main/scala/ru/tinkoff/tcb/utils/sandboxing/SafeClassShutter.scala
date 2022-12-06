package ru.tinkoff.tcb.utils.sandboxing

import org.mozilla.javascript.ClassShutter

object SafeClassShutter extends ClassShutter {
  override def visibleToScripts(fullClassName: String): Boolean = fullClassName.startsWith("adapter")
}
