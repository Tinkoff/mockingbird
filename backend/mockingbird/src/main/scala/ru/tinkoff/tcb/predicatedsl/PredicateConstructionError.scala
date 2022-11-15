package ru.tinkoff.tcb.predicatedsl

import cats.data.NonEmptyList
import io.circe.Json
import io.circe.Printer

import ru.tinkoff.tcb.utils.circe.optics.JsonOptic

sealed trait PredicateConstructionError

object PredicateConstructionError {
  implicit val show: Show[PredicateConstructionError] = Show.show[PredicateConstructionError] { s =>
    def mkError(errs: NonEmptyList[(Keyword, Json)]): String = {
      val printer = Printer.noSpaces.copy(dropNullValues = true)
      errs
        .foldLeft(List.empty[String]) { case (acc, (kw, json)) =>
          acc :+ s"""invalid op: "${kw.value}" on: ${printer.print(json)}"""
        }
        .mkString("[", ", ", "]")
    }

    s match {
      case XPathError(xpath, error) => s"""xpath: "$xpath" error:$error"""
      case SpecificationError(xpath, errors) =>
        s"""[xpath: "$xpath" errors:${mkError(errors)}]"""

      case JSpecificationError(optic, errors) =>
        s"""[optic: "$optic" errors:${mkError(errors)}]"""
    }
  }
}

case class XPathError(xpath: String, error: String) extends PredicateConstructionError
case class SpecificationError(xpath: String, errors: NonEmptyList[(Keyword, Json)]) extends PredicateConstructionError

case class JSpecificationError(optic: JsonOptic, errors: NonEmptyList[(Keyword, Json)])
    extends PredicateConstructionError
