package ru.tinkoff.tcb.mockingbird.api.request

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.utils.xml.XMLString
import ru.tinkoff.tcb.xpath.Xpath

@derive(decoder, encoder, schema)
case class XPathTestRequest(xml: XMLString, path: Xpath)
