package ru.tinkoff.tcb.utils.sandboxing

import org.mozilla.javascript.ClassShutter

class SafeClassShutter(userAllowedClasses: Set[String]) extends ClassShutter {
  override def visibleToScripts(fullClassName: String): Boolean =
    fullClassName.startsWith("adapter") ||
      allowedClasses(fullClassName) ||
      userAllowedClasses(fullClassName)

  private val allowedClasses = Set(
    "scala.collection.convert.JavaCollectionWrappers$MapWrapper",
    "scala.collection.convert.JavaCollectionWrappers$SeqWrapper"
  )
}
