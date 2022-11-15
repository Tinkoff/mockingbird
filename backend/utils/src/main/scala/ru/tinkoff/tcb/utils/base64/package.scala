package ru.tinkoff.tcb.utils

import java.nio.charset.Charset
import java.util.Base64

package object base64 {
  private val Utf8 = Charset.forName("UTF-8")

  implicit class StringB64Ops(private val text: String) extends AnyVal {
    def toBase64Bytes: Array[Byte] = Base64.getEncoder.encode(text.getBytes(Utf8))

    def toBase64String: String = new String(toBase64Bytes, Utf8)

    def bytesFromBase64String: Array[Byte] = text.getBytes(Utf8).fromBase64Bytes

    def fromBase64String: String = new String(bytesFromBase64String, Utf8)
  }

  implicit class ByteArrB64ops(private val bytes: Array[Byte]) extends AnyVal {
    def fromBase64Bytes: Array[Byte] = Base64.getDecoder.decode(bytes)

    def toBase64Bytes: Array[Byte] = Base64.getEncoder.encode(bytes)

    def toBase64String: String = new String(toBase64Bytes, Utf8)
  }
}
