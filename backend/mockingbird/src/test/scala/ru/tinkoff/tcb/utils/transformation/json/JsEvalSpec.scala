package ru.tinkoff.tcb.utils.transformation.json

import java.math.BigDecimal as JBD
import java.security.MessageDigest
import java.util.List as JList
import scala.jdk.CollectionConverters.*

import io.circe.Json
import io.circe.syntax.*
import org.scalatest.TryValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.mockingbird.config.JsSandboxConfig
import ru.tinkoff.tcb.utils.sandboxing.GraalJsSandbox
import ru.tinkoff.tcb.utils.transformation.json.js_eval.circe2js

class JsEvalSpec extends AnyFunSuite with Matchers with TryValues {
  private val sandbox = new GraalJsSandbox(JsSandboxConfig())

  test("Simple expressions") {
    val data = Json.obj("a" := Json.obj("b" := 42, "c" := "test", "d" := 1 :: 2 :: Nil))

    val res = sandbox.eval[JBD]("req.a.b", Map("req" -> data.foldWith(circe2js)))

    res.success.value shouldBe BigDecimal(42).bigDecimal

    val res2 = sandbox.eval[String]("req.a.c", Map("req" -> data.foldWith(circe2js)))

    res2.success.value shouldBe "test"

    val res3 = sandbox.eval[JList[JBD]]("req.a.d", Map("req" -> data.foldWith(circe2js)))

    res3.success.value.asScala should contain theSameElementsInOrderAs List(1, 2).map(JBD.valueOf(_))

    val res4 = sandbox.eval[JBD]("req.a.d[0]", Map("req" -> data.foldWith(circe2js)))

    res4.success.value shouldBe BigDecimal(1).bigDecimal
  }

  test("JS functions") {
    val aesSandbox = new GraalJsSandbox(JsSandboxConfig(Set("java.security.MessageDigest")))

    val etalon = MessageDigest.getInstance("SHA-1").digest("abc".getBytes)

    // https://stackoverflow.com/a/22861911/3819595
    val res = aesSandbox.eval[Array[Byte]](
      """var md = java.security.MessageDigest.getInstance("SHA-1");
        |md.digest('abc'.split('').map(c => c.charCodeAt(0)));""".stripMargin
    )

    res.success.value shouldBe etalon
  }
}
