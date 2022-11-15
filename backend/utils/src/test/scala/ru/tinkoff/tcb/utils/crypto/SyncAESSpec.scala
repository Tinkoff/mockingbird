package ru.tinkoff.tcb.utils.crypto

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class SyncAESSpec extends AnyFunSuite with Matchers {
  private val zaes = new SyncAES("TOP SECRET")

  test("Encode-Decode") {
    val (encoded, salt, iv) = zaes.encrypt("my data")
    val decoded             = zaes.decrypt(encoded, salt, iv)

    decoded shouldBe "my data"
  }
}
