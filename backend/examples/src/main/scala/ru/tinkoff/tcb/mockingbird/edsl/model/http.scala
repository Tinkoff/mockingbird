package ru.tinkoff.tcb.mockingbird.edsl.model

import enumeratum.*

import ru.tinkoff.tcb.mockingbird.edsl.model.Check.*

sealed trait HttpMethod extends EnumEntry
object HttpMethod extends Enum[HttpMethod] {
  val values = findValues
  case object Get extends HttpMethod
  case object Post extends HttpMethod
  case object Delete extends HttpMethod
}

final case class HttpResponse(code: Int, body: Option[String], headers: Seq[(String, String)])

final case class HttpRequest(
    method: HttpMethod,
    path: String,
    body: Option[String] = None,
    headers: Seq[(String, String)] = Seq.empty,
    query: Seq[(String, String)] = Seq.empty,
)

final case class HttpResponseExpected(
    code: Option[CheckInteger] = None,
    body: Option[Check] = None,
    headers: Seq[(String, CheckString)] = Seq.empty,
)
