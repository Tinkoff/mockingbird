package ru.tinkoff.tcb.mockingbird.api

import io.circe.Json
import io.circe.literal.*
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import sttp.monad.MonadError
import sttp.tapir.EndpointInput
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.vertx.zio.VertxZioServerInterpreter
import sttp.tapir.static.Resources
import sttp.tapir.static.ResourcesOptions
import sttp.tapir.static.StaticInput
import sttp.tapir.ztapir.*

import ru.tinkoff.tcb.mockingbird.build.BuildInfo
import ru.tinkoff.tcb.mockingbird.wldRuntime

class UIHttp {
  private val versionEndpoint: ZServerEndpoint[WLD, Any] =
    endpoint.get
      .in("mockingbird" / "assets" / "version.json")
      .out(jsonBody[Json])
      .zServerLogic[WLD](_ => ZIO.succeed(json"""{"version": ${BuildInfo.version}}"""))

  private val staticEndpoint: ZServerEndpoint[WLD, Any] =
    resourcesGetServerEndpoint[RIO[WLD, *]]("mockingbird" / "assets")(this.getClass.getClassLoader, "out/assets")

  private val indexEndpoint: ZServerEndpoint[WLD, Any] =
    resourcesGetServerEndpoint2("mockingbird")(
      this.getClass.getClassLoader,
      "out",
      ResourcesOptions.default.defaultResource("404" :: "index.html" :: Nil)
    )

  val http: List[Router => Route] =
    List(versionEndpoint, staticEndpoint, indexEndpoint).map(VertxZioServerInterpreter().route(_)(wldRuntime))

  private def resourcesGetServerEndpoint2[F[_]](prefix: EndpointInput[Unit])(
      classLoader: ClassLoader,
      resourcePrefix: String,
      options: ResourcesOptions[F]
  ): ServerEndpoint[Any, F] =
    ServerEndpoint.public(
      resourcesGetEndpoint(prefix).mapIn((si: StaticInput) => si.copy(path = si.path.appended("index.html")))(si =>
        si.copy(path = si.path.init)
      ),
      (m: MonadError[F]) => Resources(classLoader, resourcePrefix, options)(m)
    )
}

object UIHttp {
  def live: TaskLayer[UIHttp] =
    ZLayer.succeed(new UIHttp)
}
