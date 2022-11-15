package ru.tinkoff.tcb.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import scala.util.Random
import scala.util.matching.Regex

import ru.tinkoff.tcb.utils.time.*

package object transformation {
  val SubstRx: Regex = """\$\{(.*?)\}""".r
  val FunRx: Regex   = """%\{.*?\}""".r

  val RandStr: Regex          = """%\{randomString\((\d+)\)\}""".r
  val RandAlphabetStr: Regex  = """%\{randomString\(\"(.*?)\",\s*(\d+),\s*(\d+)\)\}""".r
  val RandNumStr: Regex       = """%\{randomNumericString\((\d+)\)\}""".r
  val RandInt: Regex          = """%\{randomInt\((\d+)\)\}""".r
  val RandIntInterval: Regex  = """%\{randomInt\((\d+),\s*(\d+)\)\}""".r
  val RandLong: Regex         = """%\{randomLong\((\d+)\)\}""".r
  val RandLongInterval: Regex = """%\{randomLong\((\d+),\s*(\d+)\)\}""".r
  val RandUUID: Regex         = """%\{UUID\}""".r

  val Today: Regex = """%\{today\((.*?)\)\}""".r
  val Now: Regex   = """%\{now\((.*?)\)\}""".r

  implicit final class TemplateTransformations(private val template: String) extends AnyVal {
    def foldTemplate[T](foldString: String => T, foldInt: Int => T, foldLong: Long => T): Option[T] =
      template match {
        case RandStr(len) =>
          Some(foldString(Random.alphanumeric.take(len.toInt).mkString))
        case RandAlphabetStr(alphabet, minLen, maxLen) =>
          Some(
            foldString(
              List.fill(Random.between(minLen.toInt, maxLen.toInt))(alphabet(Random.nextInt(alphabet.length))).mkString
            )
          )
        case RandNumStr(len) =>
          Some(foldString(Seq.fill(len.toInt)(Random.nextInt(10)).mkString))
        case RandInt(max) =>
          Some(foldInt(Random.nextInt(max.toInt)))
        case RandIntInterval(min, max) =>
          Some(foldInt(Random.between(min.toInt, max.toInt)))
        case RandLong(max) =>
          Some(foldLong(Random.nextLong(max.toLong)))
        case RandLongInterval(min, max) =>
          Some(foldLong(Random.between(min.toLong, max.toLong)))
        case RandUUID() =>
          Some(foldString(UUID.randomUUID().toString))
        case Today(Formatter(fmt)) =>
          Some(foldString(LocalDate.now().format(fmt)))
        case Now(Formatter(fmt)) =>
          Some(foldString(LocalDateTime.now().format(fmt)))
        case _ => None
      }
  }
}
