package ru.tinkoff.tcb.utils.sandboxing

import scala.reflect.ClassTag
import scala.reflect.classTag
import scala.util.Try
import scala.util.Using

import org.graalvm.polyglot.*

import ru.tinkoff.tcb.utils.instances.predicate.or.*

class GraalJsSandbox(
    classAccessRules: List[ClassAccessRule] = GraalJsSandbox.DefaultAccess,
    prelude: Option[String] = None
) {
  private val accessRule = classAccessRules.asInstanceOf[List[String => Boolean]].combineAll

  private val preludeSource = prelude.map(Source.create("js", _))

  def eval[T: ClassTag](code: String, environment: Map[String, Any] = Map.empty): Try[T] =
    Using(
      Context
        .newBuilder("js")
        .allowHostAccess(HostAccess.ALL)
        .allowHostClassLookup((t: String) => accessRule(t))
        .option("engine.WarnInterpreterOnly", "false")
        .build()
    ) { context =>
      context.getBindings("js").pipe { bindings =>
        for ((key, value) <- environment)
          bindings.putMember(key, value)
      }
      preludeSource.foreach(context.eval)
      context.eval("js", code).as(classTag[T].runtimeClass.asInstanceOf[Class[T]])
    }
}

object GraalJsSandbox {
  val DefaultAccess: List[ClassAccessRule] = List(
    ClassAccessRule.Exact("java.lang.Byte"),
    ClassAccessRule.Exact("java.lang.Boolean"),
    ClassAccessRule.Exact("java.lang.Double"),
    ClassAccessRule.Exact("java.lang.Float"),
    ClassAccessRule.Exact("java.lang.Integer"),
    ClassAccessRule.Exact("java.lang.Long"),
    ClassAccessRule.Exact("java.lang.Math"),
    ClassAccessRule.Exact("java.lang.Short"),
    ClassAccessRule.Exact("java.lang.String"),
    ClassAccessRule.Exact("java.math.BigDecimal"),
    ClassAccessRule.Exact("java.math.BigInteger"),
    ClassAccessRule.Exact("java.time.LocalDate"),
    ClassAccessRule.Exact("java.time.LocalDateTime"),
    ClassAccessRule.Exact("java.time.format.DateTimeFormatter"),
    ClassAccessRule.Exact("java.util.List"),
    ClassAccessRule.Exact("java.util.Map"),
    ClassAccessRule.Exact("java.util.Random"),
    ClassAccessRule.Exact("java.util.Set")
  )
}
