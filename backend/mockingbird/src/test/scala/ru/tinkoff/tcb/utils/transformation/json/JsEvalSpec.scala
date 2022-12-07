package ru.tinkoff.tcb.utils.transformation.json

import java.math.BigDecimal as JBD
import java.security.MessageDigest
import java.util.List as JList
import scala.jdk.CollectionConverters.*

import io.circe.Json
import io.circe.syntax.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.utils.sandboxing.RhinoJsSandbox
import ru.tinkoff.tcb.utils.transformation.json.js_eval.circe2js

class JsEvalSpec extends AnyFunSuite with Matchers {
  private val sandbox = new RhinoJsSandbox

  test("Simple expressions") {
    val data = Json.obj("a" := Json.obj("b" := 42, "c" := "test", "d" := 1 :: 2 :: Nil))

    val res = sandbox.eval[Int]("req.a.b", Map("req" -> data.foldWith(circe2js)))

    res shouldBe 42

    val res2 = sandbox.eval[String]("req.a.c", Map("req" -> data.foldWith(circe2js)))

    res2 shouldBe "test"

    val res3 = sandbox.eval[JList[JBD]]("req.a.d", Map("req" -> data.foldWith(circe2js)))

    res3.asScala should contain theSameElementsInOrderAs List(1, 2).map(JBD.valueOf(_))

    val res4 = sandbox.eval[Int]("req.a.d[0]", Map("req" -> data.foldWith(circe2js)))

    res4 shouldBe 1
  }

  test("JS functions") {
    val aesSandbox = new RhinoJsSandbox(
      allowedClasses = List(
        "java.security.MessageDigest",
        "java.security.MessageDigest$Delegate$CloneableDelegate",
        "java.lang.String",
        "java.lang.Object"
      )
    )

    val etalon = MessageDigest.getInstance("SHA-1").digest("abc".getBytes)

    val res = aesSandbox.eval[Array[Byte]](
      """var md = java.security.MessageDigest.getInstance("SHA-1");
        |md.digest((new java.lang.String("abc")).getBytes());""".stripMargin
    )

    res shouldBe etalon
  }
}
