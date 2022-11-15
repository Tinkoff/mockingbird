package ru.tinkoff.tcb.utils.xml

import scala.xml.Elem
import scala.xml.InputSource
import scala.xml.PCData
import scala.xml.TopScope
import scala.xml.factory.XMLLoader
import scala.xml.parsing.FactoryAdapter

import org.xml.sax.ext.DefaultHandler2
import org.xml.sax.ext.LexicalHandler

/*
  Хак с обработкой CDATA принадлежит перу славного индуса Kolmar
  https://stackoverflow.com/a/35483778
 */
object SafeXML extends XMLLoader[Elem] {
  // scalastyle:off import.grouping
  import javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING
  import javax.xml.parsers.SAXParser
  import javax.xml.parsers.SAXParserFactory

  override def parser: SAXParser = {
    val factory = SAXParserFactory.newInstance()
    factory.setFeature(FEATURE_SECURE_PROCESSING, true)
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    factory.setFeature("http://xml.org/sax/features/resolve-dtd-uris", false)
    factory.setXIncludeAware(false)
    factory.setNamespaceAware(false)

    factory.newSAXParser()
  }

  private def lexicalHandler(adapter: FactoryAdapter): LexicalHandler =
    new DefaultHandler2 {
      def captureCData(): Unit = {
        adapter.hStack = PCData(adapter.buffer.toString) :: adapter.hStack
        adapter.buffer.clear()
      }

      override def startCDATA(): Unit = adapter.captureText()
      override def endCDATA(): Unit   = captureCData()
    }

  override def loadXML(source: InputSource, parser: SAXParser): Elem = {
    val newAdapter = adapter

    val xmlReader = parser.getXMLReader
    xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", lexicalHandler(newAdapter))

    newAdapter.scopeStack = TopScope :: newAdapter.scopeStack
    parser.parse(source, newAdapter)
    newAdapter.scopeStack = newAdapter.scopeStack.tail

    newAdapter.rootElem.asInstanceOf[Elem]
  }
}
