package ru.tinkoff.tcb.utils.crypto

import java.nio.charset.Charset
import java.security.Key
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

import ru.tinkoff.tcb.utils.base64.*

trait AES {
  def encrypt(value: String): (String, String, String)

  def decrypt(value: String, salt: String, initializationVector: String): String
}

final class SyncAES(secret: String) extends AES {
  private val cipher                     = "AES/GCM/NoPadding"
  private val Algorithm                  = "AES"
  private val GcmAuthenticationTagLength = 128
  private val SaltLength                 = 16
  private val IvLength                   = 12

  private val random: SecureRandom = new SecureRandom()

  private val Utf8 = Charset.forName("UTF-8")

  override def encrypt(value: String): (String, String, String) = {
    val rawSalt  = randomArray(SaltLength)
    val key: Key = getAESKey(rawSalt)
    val iv: GCMParameterSpec =
      new GCMParameterSpec(GcmAuthenticationTagLength, randomArray(IvLength))

    val c = Cipher.getInstance(cipher)
    c.init(Cipher.ENCRYPT_MODE, key, iv)
    val cipherText = c.doFinal(value.getBytes(Utf8))

    (cipherText.toBase64String, rawSalt.toBase64String, iv.getIV.toBase64String)
  }

  override def decrypt(value: String, salt: String, initializationVector: String): String = {
    val key: Key  = getAESKey(salt.bytesFromBase64String)
    val gcmParams = new GCMParameterSpec(GcmAuthenticationTagLength, initializationVector.bytesFromBase64String)

    val c = Cipher.getInstance(cipher)
    c.init(Cipher.DECRYPT_MODE, key, gcmParams)
    val clearText = c.doFinal(value.bytesFromBase64String)

    new String(clearText, Utf8)
  }

  private def getAESKey(salt: Array[Byte]) = {
    val factory        = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val iterationCount = 65536
    val keyLength      = 256
    val spec           = new PBEKeySpec(secret.toCharArray, salt, iterationCount, keyLength)
    new SecretKeySpec(factory.generateSecret(spec).getEncoded, Algorithm)
  }

  private def randomArray(length: Int): Array[Byte] =
    new Array[Byte](length).tap(random.nextBytes)
}
