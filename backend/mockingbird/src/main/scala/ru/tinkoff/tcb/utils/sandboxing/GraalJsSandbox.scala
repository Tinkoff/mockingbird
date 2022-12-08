package ru.tinkoff.tcb.utils.sandboxing

import scala.reflect.ClassTag
import scala.reflect.classTag
import scala.util.Try
import scala.util.Using

import org.graalvm.polyglot.*

import ru.tinkoff.tcb.utils.instances.predicate.or.*

class GraalJsSandbox(classAccessRules: List[ClassAccessRule] = GraalJsSandbox.DefaultAccess) {
  private val accessRule = classAccessRules.asInstanceOf[List[String => Boolean]].combineAll

  def eval[T: ClassTag](code: String, environment: Map[String, Any] = Map.empty): Try[T] =
    Using(
      Context
        .newBuilder("js")
        .allowHostAccess(HostAccess.ALL)
        .allowHostClassLookup((t: String) => accessRule(t))
        .build()
    ) { context =>
      context.getBindings("js").pipe { bindings =>
        for ((key, value) <- environment)
          bindings.putMember(key, value)
      }
      context.eval("js", code).as(classTag[T].runtimeClass.asInstanceOf[Class[T]])
    }
}

object GraalJsSandbox {
  val DefaultAccess: List[ClassAccessRule] = List(
    ClassAccessRule.StartsWith("java.lang.Byte"),
    ClassAccessRule.StartsWith("java.lang.Boolean"),
    ClassAccessRule.StartsWith("java.lang.Double"),
    ClassAccessRule.StartsWith("java.lang.Float"),
    ClassAccessRule.StartsWith("java.lang.Integer"),
    ClassAccessRule.StartsWith("java.lang.Long"),
    ClassAccessRule.StartsWith("java.lang.Math"),
    ClassAccessRule.StartsWith("java.lang.Short"),
    ClassAccessRule.StartsWith("java.lang.String"),
    ClassAccessRule.StartsWith("java.math.BigDecimal"),
    ClassAccessRule.StartsWith("java.math.BigInteger"),
    ClassAccessRule.StartsWith("java.util.List"),
    ClassAccessRule.StartsWith("java.util.Map"),
    ClassAccessRule.StartsWith("java.util.Random"),
    ClassAccessRule.StartsWith("java.util.Set")
  )
}
