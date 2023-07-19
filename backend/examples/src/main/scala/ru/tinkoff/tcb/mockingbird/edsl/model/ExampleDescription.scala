package ru.tinkoff.tcb.mockingbird.edsl.model

import org.scalactic.source

final case class ExampleDescription(name: String, steps: Example[Any], pos: source.Position)
