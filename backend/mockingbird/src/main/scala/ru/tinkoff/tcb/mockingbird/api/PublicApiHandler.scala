package ru.tinkoff.tcb.mockingbird.api

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal
import scala.xml.Node

import io.circe.Json
import io.circe.parser.parse
import io.circe.syntax.*
import io.estatico.newtype.ops.*
import mouse.option.*
import sttp.client3.*
import sttp.client3.circe.*
import sttp.model.Method
import zio.interop.catz.core.*

import ru.tinkoff.tcb.logging.MDCLogging
import ru.tinkoff.tcb.mockingbird.config.ProxyConfig
import ru.tinkoff.tcb.mockingbird.dal.PersistentStateDAO
import ru.tinkoff.tcb.mockingbird.dal2.HttpStubDAO
import ru.tinkoff.tcb.mockingbird.error.*
import ru.tinkoff.tcb.mockingbird.misc.Renderable.ops.*
import ru.tinkoff.tcb.mockingbird.model.AbsentRequestBody
import ru.tinkoff.tcb.mockingbird.model.BinaryResponse
import ru.tinkoff.tcb.mockingbird.model.ByteArray
import ru.tinkoff.tcb.mockingbird.model.HttpMethod
import ru.tinkoff.tcb.mockingbird.model.HttpStubResponse
import ru.tinkoff.tcb.mockingbird.model.JsonProxyResponse
import ru.tinkoff.tcb.mockingbird.model.MultipartRequestBody
import ru.tinkoff.tcb.mockingbird.model.PersistentState
import ru.tinkoff.tcb.mockingbird.model.ProxyResponse
import ru.tinkoff.tcb.mockingbird.model.RawResponse
import ru.tinkoff.tcb.mockingbird.model.RequestBody
import ru.tinkoff.tcb.mockingbird.model.Scope
import ru.tinkoff.tcb.mockingbird.model.SimpleRequestBody
import ru.tinkoff.tcb.mockingbird.model.XmlProxyResponse
import ru.tinkoff.tcb.mockingbird.scenario.CallbackEngine
import ru.tinkoff.tcb.mockingbird.scenario.ScenarioEngine
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.utils.regex.*
import ru.tinkoff.tcb.utils.sandboxing.GraalJsSandbox
import ru.tinkoff.tcb.utils.transformation.json.*
import ru.tinkoff.tcb.utils.transformation.string.*
import ru.tinkoff.tcb.utils.transformation.xml.*
import ru.tinkoff.tcb.utils.xml.SafeXML
import ru.tinkoff.tcb.utils.xml.emptyKNode
import ru.tinkoff.tcb.utils.xttp.xml.asXML
import ru.tinkoff.tcb.xpath.SXpath

final class PublicApiHandler(
    stubDAO: HttpStubDAO[Task],
    stateDAO: PersistentStateDAO[Task],
    resolver: StubResolver,
    engine: CallbackEngine,
    implicit val jsSandbox: GraalJsSandbox,
    private val httpBackend: SttpBackend[Task, ?],
    proxyConfig: ProxyConfig
) {
  private val log = MDCLogging.`for`[WLD](this)

  def exec(
      method: HttpMethod,
      path: String,
      headers: Map[String, String],
      query: Map[String, String],
      body: RequestBody
  ): RIO[WLD, HttpStubResponse] = {
    val queryObject = Json.fromFields(query.view.mapValues(s => parse(s).getOrElse(Json.fromString(s))))
    val f           = resolver.findStubAndState(method, path, headers, queryObject, body) _

    for {
      _ <- Tracing.update(_.addToPayload("path" -> path, "method" -> method.entryName))
      (stub, stateOp) <- f(Scope.Countdown)
        .filterOrElse(_.isDefined)(f(Scope.Ephemeral).filterOrElse(_.isDefined)(f(Scope.Persistent)))
        .someOrFail(StubSearchError(s"Не удалось подобрать заглушку для [$method] $path"))
      _ <- Tracing.update(_.addToPayload("name" -> stub.name))
      seed     = stub.seed.map(_.eval)
      srb      = SimpleRequestBody.subset.getOption(body).map(_.value)
      bodyJson = stub.request.extractJson(body)
      bodyXml  = stub.request.extractXML(body)
      groups = for {
        pattern <- stub.pathPattern
        mtch    <- pattern.findFirstMatchIn(path)
      } yield pattern.groups.map(g => g -> mtch.group(g)).to(Map)
      state <- ZIO.fromOption(stateOp).orElse(PersistentState.fresh)
      data = Json.obj(
        "seed" := seed,
        "req" := bodyJson,
        "state" := state.data,
        "query" := queryObject,
        "pathParts" := groups,
        "extracted" := bodyXml.map(stub.request.runXmlExtractors),
        "headers" := headers
      )
      xdata   = bodyXml.getOrElse(emptyKNode)
      persist = stub.persist
      _ <- persist
        .cata(spec => stateDAO.upsertBySpec(state.id, spec.fill(data).fill(xdata)).map(_.successful), ZIO.succeed(true))
      _ <- persist
        .map(_.keys.map(_.path).filter(_.startsWith("_")).toVector)
        .filter(_.nonEmpty)
        .cata(_.traverse(stateDAO.createIndexForDataField), ZIO.unit)
      response <- stub.response match {
        case ProxyResponse(uri, delay, timeout) =>
          proxyRequest(method, headers, query, body)(uri.substitute(data), delay, timeout)
        case JsonProxyResponse(uri, patch, delay, timeout) =>
          jsonProxyRequest(method, headers, query, body, data)(uri.substitute(data), patch, delay, timeout)
        case XmlProxyResponse(uri, patch, delay, timeout) =>
          xmlProxyRequest(
            method,
            headers,
            query,
            body,
            data,
            srb.map(SafeXML.loadString).getOrElse(scala.xml.Comment("No data"))
          )(
            uri.substitute(data),
            patch,
            delay,
            timeout
          )
        case _ =>
          ZIO.succeed(
            if (stub.response.isTemplate) {
              HttpStubResponse.jsonBody
                .updateF(_.substitute(data).substitute(xdata))
                .andThen(HttpStubResponse.xmlBody.updateF(_.substitute(data).substitute(xdata)))(stub.response)
            } else stub.response
          )
      }
      _ <- ZIO.when(stub.scope == Scope.Countdown)(stubDAO.incTimesById(stub.id, -1))
      _ <- ZIO.when(stub.callback.isDefined)(
        engine
          .recurseCallback(state, stub.callback.get, data, xdata)
          .catchSomeDefect { case NonFatal(ex) =>
            ZIO.fail(ex)
          }
          .catchSome { case NonFatal(ex) =>
            log.errorCause("Ошибка при выполнении колбэка", ex)
          }
          .forkDaemon
      )
    } yield response
  }

  private def proxyRequest(
      method: HttpMethod,
      headers: Map[String, String],
      query: Map[String, String],
      body: RequestBody
  )(uri: String, delay: Option[FiniteDuration], timeout: Option[FiniteDuration]): RIO[WLD, HttpStubResponse] = {
    val requestUri = uri"$uri".pipe(query.foldLeft(_) { case (u, (key, value)) => u.addParam(key, value) })
    log.debug(s"Received headers: ${headers.keys.mkString(", ")}") *> basicRequest
      .headers(headers -- proxyConfig.excludedRequestHeaders)
      .method(Method(method.entryName), requestUri)
      .pipe(rt =>
        body match {
          case AbsentRequestBody        => rt
          case SimpleRequestBody(value) => rt.body(value)
          case MultipartRequestBody(value) =>
            rt.multipartBody[Any](
              value.map(part =>
                multipart(part.name, part.body)
                  .pipe(newPart =>
                    part.headers.foldLeft(newPart) { case (acc, header) =>
                      acc.header(header.name, header.value, true)
                    }
                  )
              )
            )
        }
      )
      .response(asByteArrayAlways)
      .readTimeout(timeout.getOrElse(1.minute.asScala))
      .send(httpBackend)
      .map { response =>
        BinaryResponse(
          response.code.code,
          response.headers
            .filterNot(h => proxyConfig.excludedResponseHeaders(h.name))
            .map(h => h.name -> h.value)
            .toMap,
          response.body.coerce[ByteArray],
          delay
        )
      }
  }

  private def jsonProxyRequest(
      method: HttpMethod,
      headers: Map[String, String],
      query: Map[String, String],
      body: RequestBody,
      data: Json
  )(
      uri: String,
      patch: Map[JsonOptic, String],
      delay: Option[FiniteDuration],
      timeout: Option[FiniteDuration]
  ): RIO[WLD, HttpStubResponse] = {
    val requestUri = uri"$uri".pipe(query.foldLeft(_) { case (u, (key, value)) =>
      u.addParam(key, value)
    })
    log.debug(s"Received headers: ${headers.keys.mkString(", ")}") *> basicRequest
      .headers(headers -- proxyConfig.excludedRequestHeaders)
      .method(Method(method.entryName), requestUri)
      .pipe(rt =>
        body match {
          case AbsentRequestBody        => rt
          case SimpleRequestBody(value) => rt.body(value)
          case MultipartRequestBody(value) =>
            rt.multipartBody[Any](
              value.map(part =>
                multipart(part.name, part.body)
                  .pipe(newPart =>
                    part.headers.foldLeft(newPart) { case (acc, header) => acc.header(header.name, header.value, true) }
                  )
              )
            )
        }
      )
      .response(asJsonAlways[Json])
      .readTimeout(timeout.getOrElse(1.minute.asScala))
      .send(httpBackend)
      .map { response =>
        response.body match {
          case Right(jsonResponse) =>
            RawResponse(
              response.code.code,
              response.headers
                .filterNot(h => proxyConfig.excludedResponseHeaders(h.name))
                .map(h => h.name -> h.value)
                .toMap,
              jsonResponse.patch(data, patch).noSpaces,
              delay
            )
          case Left(error) =>
            RawResponse(500, Map(), error.body, delay)
        }
      }
  }

  private def xmlProxyRequest(
      method: HttpMethod,
      headers: Map[String, String],
      query: Map[String, String],
      body: RequestBody,
      jData: Json,
      xData: Node
  )(
      uri: String,
      patch: Map[SXpath, String],
      delay: Option[FiniteDuration],
      timeout: Option[FiniteDuration]
  ): RIO[WLD, HttpStubResponse] = {
    val requestUri = uri"$uri".pipe(query.foldLeft(_) { case (u, (key, value)) =>
      u.addParam(key, value)
    })
    log.debug(s"Received headers: ${headers.keys.mkString(", ")}") *> basicRequest
      .headers(headers -- proxyConfig.excludedRequestHeaders)
      .method(Method(method.entryName), requestUri)
      .pipe(rt =>
        body match {
          case AbsentRequestBody        => rt
          case SimpleRequestBody(value) => rt.body(value)
          case MultipartRequestBody(value) =>
            rt.multipartBody[Any](
              value.map(part =>
                multipart(part.name, part.body)
                  .pipe(newPart =>
                    part.headers.foldLeft(newPart) { case (acc, header) => acc.header(header.name, header.value, true) }
                  )
              )
            )
        }
      )
      .response(asXML)
      .readTimeout(timeout.getOrElse(1.minute.asScala))
      .send(httpBackend)
      .map { response =>
        response.body match {
          case Right(xmlResponse) =>
            RawResponse(
              response.code.code,
              response.headers
                .filterNot(h => proxyConfig.excludedResponseHeaders(h.name))
                .map(h => h.name -> h.value)
                .toMap,
              xmlResponse.patchFromValues(jData, xData, patch.map { case (k, v) => k.toZoom -> v }).toString(),
              delay
            )
          case Left(error) =>
            RawResponse(500, Map(), error, delay)
        }
      }
  }
}

object PublicApiHandler {
  val live = ZLayer {
    for {
      hsd        <- ZIO.service[HttpStubDAO[Task]]
      ssd        <- ZIO.service[PersistentStateDAO[Task]]
      resolver   <- ZIO.service[StubResolver]
      engine     <- ZIO.service[ScenarioEngine]
      jsSandbox  <- ZIO.service[GraalJsSandbox]
      sttpClient <- ZIO.service[SttpBackend[Task, Any]]
      proxyCfg   <- ZIO.service[ProxyConfig]
    } yield new PublicApiHandler(hsd, ssd, resolver, engine, jsSandbox, sttpClient, proxyCfg)
  }
}
