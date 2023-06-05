package ru.tinkoff.tcb.mockingbird.api

import io.circe.Json
import io.circe.literal.*
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import sttp.monad.MonadError
import sttp.tapir.EndpointInput
import sttp.tapir.files.FilesOptions
import sttp.tapir.files.Resources
import sttp.tapir.files.staticResourcesGetEndpoint
import sttp.tapir.files.staticResourcesGetServerEndpoint
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.vertx.zio.VertxZioServerInterpreter
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
    staticResourcesGetServerEndpoint[RIO[WLD, *]]("mockingbird" / "assets")(this.getClass.getClassLoader, "out/assets")

  private val indexEndpoint: ZServerEndpoint[WLD, Any] =
    resourcesGetServerEndpoint2("mockingbird")(
      this.getClass.getClassLoader,
      "out",
      FilesOptions.default.defaultFile("404" :: "index.html" :: Nil)
    )

  val http: List[Router => Route] =
    List(versionEndpoint, staticEndpoint, indexEndpoint).map(VertxZioServerInterpreter().route(_)(wldRuntime))

  private def resourcesGetServerEndpoint2[F[_]](prefix: EndpointInput[Unit])(
      classLoader: ClassLoader,
      resourcePrefix: String,
      options: FilesOptions[F]
  ): ServerEndpoint[Any, F] =
    ServerEndpoint.public(
      staticResourcesGetEndpoint(prefix).mapIn(si => si.copy(path = si.path.appended("index.html")))(si =>
        si.copy(path = si.path.init)
      ),
      (m: MonadError[F]) => Resources.get(classLoader, resourcePrefix, options)(m)
    )
}

object UIHttp {
  def live: TaskLayer[UIHttp] =
    ZLayer.succeed(new UIHttp)
}
