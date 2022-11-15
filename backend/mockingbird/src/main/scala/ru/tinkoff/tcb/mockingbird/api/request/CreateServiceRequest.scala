package ru.tinkoff.tcb.mockingbird.api.request

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.*
import eu.timepit.refined.collection.*
import eu.timepit.refined.string.*
import io.circe.refined.*
import sttp.tapir.codec.refined.*
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.generic.PropSubset
import ru.tinkoff.tcb.mockingbird.model.Service

@derive(decoder, encoder, schema)
case class CreateServiceRequest(
    suffix: String Refined And[NonEmpty, MatchesRegex["[\\w-]+"]],
    name: String Refined NonEmpty
)

object CreateServiceRequest {
  implicitly[PropSubset[CreateServiceRequest, Service]]
}
