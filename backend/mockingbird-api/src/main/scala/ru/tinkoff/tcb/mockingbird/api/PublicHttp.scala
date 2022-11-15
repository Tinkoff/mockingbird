package ru.tinkoff.tcb.mockingbird.api

import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import sttp.model.Header
import sttp.tapir.server.vertx.zio.VertxZioServerInterpreter
import sttp.tapir.server.vertx.zio.VertxZioServerOptions
import sttp.tapir.swagger.SwaggerUIOptions
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.*
import zio.Duration

import ru.tinkoff.tcb.mockingbird.api.exec.*
import ru.tinkoff.tcb.mockingbird.build.BuildInfo
import ru.tinkoff.tcb.mockingbird.model.BinaryResponse
import ru.tinkoff.tcb.mockingbird.model.HttpMethod
import ru.tinkoff.tcb.mockingbird.model.HttpStubResponse
import ru.tinkoff.tcb.mockingbird.model.JsonProxyResponse
import ru.tinkoff.tcb.mockingbird.model.JsonResponse
import ru.tinkoff.tcb.mockingbird.model.ProxyResponse
import ru.tinkoff.tcb.mockingbird.model.RawResponse
import ru.tinkoff.tcb.mockingbird.model.XmlProxyResponse
import ru.tinkoff.tcb.mockingbird.model.XmlResponse
import ru.tinkoff.tcb.mockingbird.wldRuntime

final class PublicHttp(handler: PublicApiHandler) {
  private val endpointsWithoutBody = List(getEndpoint, headEndpoint, optionsEndpoint, deleteEndpoint)
  private val endpointsWithBody    = List(postEndpoint, putEndpoint, patchEndpoint)

  private val withoutBody =
    endpointsWithoutBody
      .map(_.zServerLogic[WLD]((handle(_, _, _, _, "")).tupled))
  private val withBody =
    endpointsWithBody
      .map(_.zServerLogic[WLD]((handle _).tupled))

  private val swaggerEndpoints =
    SwaggerInterpreter(
      swaggerUIOptions = SwaggerUIOptions(
        "api" :: "mockingbird" :: "swagger" :: Nil,
        "docs.yaml",
        Nil,
        useRelativePaths = false
      )
    ).fromEndpoints[RIO[WLD, *]](endpointsWithoutBody ++ endpointsWithBody, "Mockingbird", BuildInfo.version)

  private val options =
    VertxZioServerOptions.customiseInterceptors[WLD].unsupportedMediaTypeInterceptor(None).options

  val http: List[Router => Route] =
    (withoutBody ++ withBody ++ swaggerEndpoints).map(VertxZioServerInterpreter(options).route(_)(wldRuntime))

  private def handle(
      method: HttpMethod,
      path: String,
      headers: Map[String, String],
      query: Map[String, String],
      body: String
  ): ZIO[WLD, Throwable, (List[Header], HttpStubResponse)] =
    handler
      .exec(method, path, headers, query, body)
      .tap(_.delay.fold[UIO[Unit]](ZIO.unit)(fd => ZIO.sleep(Duration.fromScala(fd))))
      .map {
        case r @ RawResponse(_, headers, _, _) =>
          (headers.map { case (name, value) => Header(name, value) }.to(List), r)
        case j @ JsonResponse(_, headers, _, _) =>
          (headers.map { case (name, value) => Header(name, value) }.to(List), j)
        case x @ XmlResponse(_, headers, _, _) =>
          (headers.map { case (name, value) => Header(name, value) }.to(List), x)
        case b @ BinaryResponse(_, headers, _, _) =>
          (headers.map { case (name, value) => Header(name, value) }.to(List), b)
        case p @ ProxyResponse(_, _, _)         => (Nil, p)
        case jp @ JsonProxyResponse(_, _, _, _) => (Nil, jp)
        case xp @ XmlProxyResponse(_, _, _, _)  => (Nil, xp)
      }
}

object PublicHttp {
  def live: RLayer[PublicApiHandler, PublicHttp] = ZLayer.fromFunction(new PublicHttp(_))
}
