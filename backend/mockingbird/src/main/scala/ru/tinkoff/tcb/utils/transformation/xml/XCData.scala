package ru.tinkoff.tcb.utils.transformation.xml

import scala.util.Try
import scala.xml.Node

import ru.tinkoff.tcb.utils.xml.SafeXML

object XCData {
  def unapply(arg: String): Option[Node] = Try(SafeXML.loadString(arg)).toOption
}
