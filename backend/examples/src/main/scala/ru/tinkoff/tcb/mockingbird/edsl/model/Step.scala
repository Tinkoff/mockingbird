package ru.tinkoff.tcb.mockingbird.edsl.model

import org.scalactic.source

sealed trait Step[T]

final case class Describe(text: String, pos: source.Position) extends Step[Unit]

final case class SendHttp[R](
    request: HttpRequest,
    pos: source.Position,
) extends Step[R]

final case class CheckHttp[R](
    response: R,
    expects: HttpResponseExpected,
    pos: source.Position,
) extends Step[HttpResponse]
