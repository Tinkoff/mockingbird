package ru.tinkoff.tcb.utils

import scala.util.matching.Regex

package object transformation {
  val SubstRx: Regex = """\$\{(.*?)\}""".r
  val FunRx: Regex   = """%\{.*?\}""".r
  val CodeRx: Regex  = """%\{(.*?)\}""".r

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
}
