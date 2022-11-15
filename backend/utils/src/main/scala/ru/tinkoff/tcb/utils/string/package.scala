package ru.tinkoff.tcb.utils

import java.util.Locale.ENGLISH

package object string {
  implicit class ExtStringOps(private val text: String) extends AnyVal {

    /**
     * Конвертирует camelCase в camel_case
     */
    def camel2Underscore: String =
      text.drop(1).foldLeft(text.headOption.map(ch => s"${ch.toLower}") getOrElse "") {
        case (acc, c) if c.isUpper => acc + "_" + c.toLower
        case (acc, c)              => acc + c
      }

    /**
     * Конвертирует snake_case в snakeCase
     */
    def underscore2Camel: String =
      camelize(text)

    /**
     * Конвертирует snake_case в SnakeCase
     */
    def underscore2UpperCamel: String =
      pascalize(text)

    private def camelize(word: String): String =
      if (word.nonEmpty) {
        val w = pascalize(word)
        w.substring(0, 1).toLowerCase(ENGLISH) + w.substring(1)
      } else {
        word
      }

    private def pascalize(word: String): String =
      word
        .split("_")
        .map(s => s.substring(0, 1).toUpperCase(ENGLISH) + s.substring(1).toLowerCase(ENGLISH))
        .mkString

    def nonEmptyString: Option[String] = if (text.isEmpty) None else Some(text)

    def insertAt(position: Int, insertion: String): String = {
      val (fst, snd) = text.splitAt(position)
      fst + insertion + snd
    }

    def replaceAt(position: Int, newChar: Char): String =
      s"${text.take(position)}$newChar${text.substring(position + 1)}"
  }
}
