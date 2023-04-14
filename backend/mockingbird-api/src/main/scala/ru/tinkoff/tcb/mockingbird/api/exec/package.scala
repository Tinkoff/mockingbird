package ru.tinkoff.tcb.mockingbird.api

import java.nio.charset.StandardCharsets

import sttp.model.Header
import sttp.tapir.*

import ru.tinkoff.tcb.mockingbird.api.input.*
import ru.tinkoff.tcb.mockingbird.codec.*
import ru.tinkoff.tcb.mockingbird.model.AbsentRequestBody
import ru.tinkoff.tcb.mockingbird.model.HttpStubResponse
import ru.tinkoff.tcb.mockingbird.model.MultipartRequestBody
import ru.tinkoff.tcb.mockingbird.model.RequestBody
import ru.tinkoff.tcb.mockingbird.model.SimpleRequestBody
import ru.tinkoff.tcb.mockingbird.model.StubCode

package object exec {
  private val baseEndpoint: PublicEndpoint[Unit, Throwable, Unit, Any] =
    endpoint.in("api" / "mockingbird" / "exec").errorOut(plainBody[Throwable])

  private val baseMultipartEndpoint: PublicEndpoint[Unit, Throwable, Unit, Any] =
    endpoint.in("api" / "mockingbird" / "execmp").errorOut(plainBody[Throwable])

  private val variants = StatusCodes.all.map { sc =>
    oneOfVariantValueMatcher(sc, binaryBody(RawBodyType.ByteArrayBody)[HttpStubResponse]) {
      case StubCode(rc) if rc == sc.code =>
        true
    }
  }

  private val withBody: PublicEndpoint[ExecInputB, Throwable, (List[Header], HttpStubResponse), Any] =
    baseEndpoint
      .in(execInput)
      .in(
        binaryBody(RawBodyType.ByteArrayBody)[Option[String]]
          .map[RequestBody]((_: Option[String]).fold[RequestBody](AbsentRequestBody)(SimpleRequestBody(_)))(
            SimpleRequestBody.subset.getOption(_).map(_.value)
          )
      )
      .out(headers)
      .out(oneOf(variants.head, variants.tail*))

  private val withMultipartBody =
    baseMultipartEndpoint
      .in(execInput)
      .in(
        multipartBody
          .map(_.map(part => part.copy[String](body = new String(part.body, StandardCharsets.UTF_8))))(
            _.map(part => part.copy(body = part.body.getBytes(StandardCharsets.UTF_8)))
          )
          .map[RequestBody](MultipartRequestBody(_))(MultipartRequestBody.subset.getOption(_).get.value)
      )
      .out(headers)
      .out(oneOf(variants.head, variants.tail*))

  val getEndpoint: PublicEndpoint[
    ExecInputB,
    Throwable,
    (List[Header], HttpStubResponse),
    Any
  ] = withBody.get

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
    ExecInputB,
    Throwable,
    (List[Header], HttpStubResponse),
    Any
  ] = withBody.delete

  val patchEndpoint: PublicEndpoint[
    ExecInputB,
    Throwable,
    (List[Header], HttpStubResponse),
    Any
  ] = withBody.patch

  val headEndpoint: PublicEndpoint[
    ExecInputB,
    Throwable,
    (List[Header], HttpStubResponse),
    Any
  ] = withBody.head

  val optionsEndpoint: PublicEndpoint[
    ExecInputB,
    Throwable,
    (List[Header], HttpStubResponse),
    Any
  ] = withBody.options

  // Multipart endpoints

  val postMultipartEndpoint: PublicEndpoint[
    ExecInputB,
    Throwable,
    (List[Header], HttpStubResponse),
    Any
  ] = withMultipartBody.post

  val putMultipartEndpoint: PublicEndpoint[
    ExecInputB,
    Throwable,
    (List[Header], HttpStubResponse),
    Any
  ] = withMultipartBody.put

  val patchMultipartEndpoint: PublicEndpoint[
    ExecInputB,
    Throwable,
    (List[Header], HttpStubResponse),
    Any
  ] = withMultipartBody.patch
}
