package ru.tinkoff.tcb.mockingbird.api.request

import derevo.circe.decoder
import derevo.circe.encoder
import derevo.derive
import io.circe.Json
import sttp.tapir.derevo.schema

import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.protocol.json.*
import ru.tinkoff.tcb.protocol.schema.*
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic

@derive(encoder, decoder, schema)
final case class SearchRequest(query: Map[JsonOptic, Map[Keyword.Json, Json]])
