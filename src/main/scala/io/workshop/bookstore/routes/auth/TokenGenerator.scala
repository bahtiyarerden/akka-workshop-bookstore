package io.workshop.bookstore.routes.auth

import java.security.{ MessageDigest, SecureRandom }

object TokenGenerator {

  val TOKEN_LENGTH = 45
  val TOKEN_CHARS  = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_.-"
  val secureRandom = new SecureRandom()

  private def toHex(bytes: Array[Byte]): String = bytes.map("%02x".format(_)).mkString("")

  private def sha(s: String): String =
    toHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8")))

  private def generateToken(tokenLength: Int): String = {
    val charLen = TOKEN_CHARS.length()

    @scala.annotation.tailrec
    def generateTokenAccumulator(accumulator: String, number: Int): String =
      if (number == 0) accumulator
      else generateTokenAccumulator(accumulator + TOKEN_CHARS(secureRandom.nextInt(charLen)).toString, number - 1)

    generateTokenAccumulator("", tokenLength)
  }

  def generateSHAToken(tokenPrefix: String): String =
    sha(tokenPrefix + System.nanoTime() + generateToken(TOKEN_LENGTH))

}
