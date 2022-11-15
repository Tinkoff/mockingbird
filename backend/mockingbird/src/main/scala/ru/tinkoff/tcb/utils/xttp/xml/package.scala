package ru.tinkoff.tcb.utils.xttp

import scala.xml.Node

import sttp.client3.ResponseAs
import sttp.client3.asString

import ru.tinkoff.tcb.utils.xml.SafeXML

package object xml {
  def asXML: ResponseAs[Either[String, Node], Any] = asString.map(_.map(SafeXML.loadString))
}
