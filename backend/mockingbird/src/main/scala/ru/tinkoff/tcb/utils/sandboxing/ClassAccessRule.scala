package ru.tinkoff.tcb.utils.sandboxing

sealed trait ClassAccessRule extends (String => Boolean)

object ClassAccessRule {
  case class Exact(className: String) extends ClassAccessRule {
    override def apply(arg: String): Boolean = arg == className
  }

  case class StartsWith(prefix: String) extends ClassAccessRule {
    override def apply(arg: String): Boolean = arg.startsWith(prefix)
  }
}
