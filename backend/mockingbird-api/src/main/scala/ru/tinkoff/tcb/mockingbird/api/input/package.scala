package ru.tinkoff.tcb.mockingbird.api

import sttp.model.Header
import sttp.model.Method
import sttp.model.QueryParams
import sttp.tapir.*

import ru.tinkoff.tcb.mockingbird.model.HttpMethod

package object input {
  private[api] type ExecInput  = (HttpMethod, String, Map[String, String], Map[String, String])
  private[api] type ExecInputB = (HttpMethod, String, Map[String, String], Map[String, String], String)

  private[api] val execInput: EndpointInput[ExecInput] =
    extractFromRequest(_.method)
      .map(m => HttpMethod.withNameInsensitive(m.method))(m => Method.unsafeApply(m.entryName))
      .and(paths.map(_.mkString("/", "/", ""))(_.split("/").to(List)))
      .and(
        extractFromRequest(_.headers)
          .map(_.map(h => h.name -> h.value).to(Map))(_.map { case (name, value) => Header(name, value) }.to(Seq))
      )
      .and(
        queryParams.map(_.toMap)(QueryParams.fromMap)
      )
}
