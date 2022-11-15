package ru.tinkoff.tcb

import javax.xml.xpath.XPathFactory

import advxml.transform.XmlZoom
import advxml.xpath.*
import kantan.xpath.XPathExpression

package object xpath {
  private[xpath] val xPathFactory = XPathFactory.newInstance()

  object Xexpr {
    def unapply(xpath: String): Option[XPathExpression] =
      Xpath.fromString(xpath).map(_.toXPathExpr).toOption
  }

  object XZoom {
    def unapply(xpath: String): Option[XmlZoom] =
      XmlZoom.fromXPath(xpath).toOption
  }
}
