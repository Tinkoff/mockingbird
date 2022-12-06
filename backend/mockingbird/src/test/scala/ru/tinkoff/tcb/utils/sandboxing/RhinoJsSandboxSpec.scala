package ru.tinkoff.tcb.utils.sandboxing

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

import org.mozilla.javascript.EcmaError
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class RhinoJsSandboxSpec extends AnyFunSuite with Matchers {
  private val sandbox = new RhinoJsSandbox

  test("Eval simple arithmetics") {
    sandbox.eval[Int]("1 + 2") shouldBe 3
  }

  test("Java classes are inaccessable") {
    assertThrows[EcmaError] {
      sandbox.eval[Int]("java.lang.System.out.println('hello');")
    }
  }

  test("Eval with context") {
    sandbox.eval[Int]("a + b", Map("a" -> 1, "b" -> 2)) shouldBe 3
  }

  test("Run time limit test") {
    val limitedSandbox = new RhinoJsSandbox(maxRunTime = Some(FiniteDuration.apply(1, TimeUnit.SECONDS)))

    assertThrows[ScriptDurationException] {
      limitedSandbox.eval[Int]("while (true) { }")
    }
  }

  test("Instruction limit test") {
    val limitedSandbox = new RhinoJsSandbox(maxInstructions = Some(100))

    assertThrows[ScriptCPUAbuseException] {
      limitedSandbox.eval[Int]("while (true) { }")
    }
  }

  test("Evaluations should not have shared data") {
    sandbox.eval[Unit]("a = 42;")
    assertThrows[EcmaError] {
      sandbox.eval[Int]("a")
    }
  }
}
