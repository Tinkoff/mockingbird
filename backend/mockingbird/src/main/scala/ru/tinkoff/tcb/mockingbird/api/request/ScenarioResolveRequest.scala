package ru.tinkoff.tcb.mockingbird.api.request

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.mockingbird.model.SourceConfiguration
import ru.tinkoff.tcb.utils.id.SID

@derive(decoder, encoder, schema)
final case class ScenarioResolveRequest(source: SID[SourceConfiguration], message: String)
