package ru.tinkoff.tcb.utils.sandboxing

import scala.reflect.ClassTag
import scala.reflect.classTag
import scala.util.Try
import scala.util.Using

import org.graalvm.polyglot.*

import ru.tinkoff.tcb.mockingbird.config.JsSandboxConfig

class GraalJsSandbox(
    jsSandboxConfig: JsSandboxConfig,
    prelude: Option[String] = None
) {
  private val allowedClasses = GraalJsSandbox.DefaultAccess ++ jsSandboxConfig.allowedClasses
  private val preludeSource  = prelude.map(Source.create("js", _))

  def eval[T: ClassTag](code: String, environment: Map[String, Any] = Map.empty): Try[T] =
    Using(
      Context
        .newBuilder("js")
        .allowHostAccess(HostAccess.ALL)
        .allowHostClassLookup((t: String) => allowedClasses(t))
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
  val live: URLayer[Option[String] & JsSandboxConfig, GraalJsSandbox] = ZLayer {
    for {
      sandboxConfig <- ZIO.service[JsSandboxConfig]
      prelude       <- ZIO.service[Option[String]]
    } yield new GraalJsSandbox(sandboxConfig, prelude)
  }

  val DefaultAccess: Set[String] = Set(
    "java.lang.Byte",
    "java.lang.Boolean",
    "java.lang.Double",
    "java.lang.Float",
    "java.lang.Integer",
    "java.lang.Long",
    "java.lang.Math",
    "java.lang.Short",
    "java.lang.String",
    "java.math.BigDecimal",
    "java.math.BigInteger",
    "java.time.LocalDate",
    "java.time.LocalDateTime",
    "java.time.format.DateTimeFormatter",
    "java.util.List",
    "java.util.Map",
    "java.util.Random",
    "java.util.Set"
  )
}
