package ru.tinkoff.tcb.mockingbird.api

import sttp.model.Header
import sttp.tapir.*

import ru.tinkoff.tcb.mockingbird.api.input.*
import ru.tinkoff.tcb.mockingbird.codec.*
import ru.tinkoff.tcb.mockingbird.model.HttpMethod
import ru.tinkoff.tcb.mockingbird.model.HttpStubResponse
import ru.tinkoff.tcb.mockingbird.model.StubCode

package object exec {
  private val baseEndpoint: PublicEndpoint[Unit, Throwable, Unit, Any] =
    endpoint.in("api" / "mockingbird" / "exec").errorOut(plainBody[Throwable])

  private val variants = StatusCodes.all.map { sc =>
    oneOfVariantValueMatcher(sc, binaryBody(RawBodyType.ByteArrayBody)[HttpStubResponse]) {
      case StubCode(rc) if rc == sc.code =>
        true
    }
  }

  private val bodylessEndpoint: PublicEndpoint[
    ExecInput,
    Throwable,
    (List[Header], HttpStubResponse),
    Any
  ] = baseEndpoint
    .in(execInput)
    .out(headers)
    .out(oneOf(variants.head, variants.tail*))

  private val withBody: PublicEndpoint[ExecInputB, Throwable, (List[Header], HttpStubResponse), Any] =
    baseEndpoint
      .in(execInput)
      .in(binaryBody(RawBodyType.ByteArrayBody)[String])
      .out(headers)
      .out(oneOf(variants.head, variants.tail*))

  val getEndpoint: PublicEndpoint[
    (HttpMethod, String, Map[String, String], Map[String, String]),
    Throwable,
    (List[Header], HttpStubResponse),
    Any
  ] = bodylessEndpoint.get

  val postEndpoint: PublicEndpoint[
    ExecInputB,
    Throwable,
    (List[Header], HttpStubResponse),
    Any
  ] = withBody.post

  val putEndpoint: PublicEndpoint[
    ExecInputB,
    Throwable,
    (List[Header], HttpStubResponse),
    Any
  ] = withBody.put

  val deleteEndpoint: PublicEndpoint[
    ExecInput,
    Throwable,
    (List[Header], HttpStubResponse),
    Any
  ] = bodylessEndpoint.delete

  val patchEndpoint: PublicEndpoint[
    ExecInputB,
    Throwable,
    (List[Header], HttpStubResponse),
    Any
  ] = withBody.patch

  val headEndpoint: PublicEndpoint[
    ExecInput,
    Throwable,
    (List[Header], HttpStubResponse),
    Any
  ] = bodylessEndpoint.head

  val optionsEndpoint: PublicEndpoint[
    ExecInput,
    Throwable,
    (List[Header], HttpStubResponse),
    Any
  ] = bodylessEndpoint.options
}
