package ru.tinkoff.tcb.mockingbird.api

import io.micrometer.prometheus.PrometheusMeterRegistry
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import sttp.tapir.PublicEndpoint
import sttp.tapir.server.vertx.zio.VertxZioServerInterpreter
import sttp.tapir.ztapir.*

import ru.tinkoff.tcb.mockingbird.wldRuntime

class MetricsHttp(registry: PrometheusMeterRegistry) {
  private val metricsEndpoint: PublicEndpoint[Unit, Unit, String, Any] =
    endpoint.get.in("metrics").out(stringBody)

  val http: Router => Route =
    VertxZioServerInterpreter()
      .route(metricsEndpoint.zServerLogic[WLD](_ => ZIO.succeed(registry.scrape())))(wldRuntime)
}

object MetricsHttp {
  def live: RLayer[PrometheusMeterRegistry, MetricsHttp] = ZLayer.fromFunction(new MetricsHttp(_))
}
