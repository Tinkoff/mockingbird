package ru.tinkoff.tcb.mockingbird.api

import scala.jdk.CollectionConverters.*

import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.CorsHandler
import mouse.ignore
import sttp.tapir.server.vertx.zio.VertxZioServerInterpreter.*
import zio.managed.*

import ru.tinkoff.tcb.mockingbird.config.ServerConfig

object WebAPI {
  val managed: ZManaged[
    WLD & PublicHttp & AdminHttp & UIHttp & MetricsHttp & ServerConfig,
    Throwable,
    HttpServer
  ] = for {
    serverConfig <- ZIO.service[ServerConfig].toManaged
    publicAPI    <- ZIO.service[PublicHttp].toManaged
    adminAPI     <- ZIO.service[AdminHttp].toManaged
    ui           <- ZIO.service[UIHttp].toManaged
    metrics      <- ZIO.service[MetricsHttp].toManaged
    server <- ZManaged.acquireReleaseWith(ZIO.attempt {
      val vertx         = Vertx.vertx()
      val serverOptions = new HttpServerOptions().setMaxFormAttributeSize(256 * 1024)
      val server        = vertx.createHttpServer(serverOptions)
      val router        = Router.router(vertx)
      router
        .route()
        .path("/api/internal/mockingbird/v*")
        .handler(
          CorsHandler
            .create()
            .addOrigins(serverConfig.allowedOrigins.asJava)
            .allowedMethods(Set(HttpMethod.GET, HttpMethod.POST, HttpMethod.PATCH, HttpMethod.DELETE).asJava)
        )
      adminAPI.http.foreach(_(router))
      publicAPI.http.foreach(_(router))
      ui.http.foreach(_(router))
      metrics.http(router)
      serverConfig.healthCheckRoute.foreach { url =>
        router.route().path(url).handler { ctx =>
          val response = ctx.response()
          response.setStatusCode(200)
          ignore(response.end().result())
        }
      }
      server.requestHandler(router).listen(serverConfig.port)
    } flatMap (_.asRIO)) { server =>
      ZIO.attempt(server.close()).flatMap(_.asRIO).orDie
    }
  } yield server
}
