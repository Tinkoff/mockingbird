package ru.tinkoff.tcb.utils.sandboxing

import org.graalvm.polyglot.PolyglotException
import org.scalatest.TryValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class GraalJsSandboxSpec extends AnyFunSuite with Matchers with TryValues {
  private val sandbox = new GraalJsSandbox

  test("Eval simple arithmetics") {
    sandbox.eval[Int]("1 + 2").success.value shouldBe 3
  }

  test("Java classes are inaccessable") {
    sandbox.eval[Any]("java.lang.System.out.println('hello');").failure.exception shouldBe a[PolyglotException]
  }

  test("Eval with context") {
    sandbox.eval[Int]("a + b", Map("a" -> 1, "b" -> 2)).success.value shouldBe 3
  }

  test("Evaluations should not have shared data") {
    sandbox.eval[Any]("a = 42;").success
    sandbox.eval[Int]("a").failure.exception shouldBe a[PolyglotException]
  }
}
