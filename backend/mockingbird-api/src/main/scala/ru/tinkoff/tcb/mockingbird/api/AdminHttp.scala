package ru.tinkoff.tcb.mockingbird.api

import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import sttp.tapir.server.vertx.zio.VertxZioServerInterpreter
import sttp.tapir.server.vertx.zio.VertxZioServerOptions
import sttp.tapir.swagger.SwaggerUIOptions
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir.*

import ru.tinkoff.tcb.mockingbird.api.admin.*
import ru.tinkoff.tcb.mockingbird.build.BuildInfo
import ru.tinkoff.tcb.mockingbird.config.ServerConfig
import ru.tinkoff.tcb.mockingbird.model.AbsentRequestBody
import ru.tinkoff.tcb.mockingbird.wldRuntime

final class AdminHttp(config: ServerConfig, handler: AdminApiHandler) {
  private val allEndpoints = List(
    fetchStates,
    testXPath,
    fetchServices,
    createService,
    getService,
    fetchStubs,
    createHttpStub,
    getStub,
    updateStub,
    deleteStub,
    fetchScenarios,
    createScenario,
    getScenario,
    updateScenario,
    deleteScenario,
    getLabels,
    fetchGrpcStubs,
    createGrpcStub,
    getGrpcStub,
    deleteGrpcStub,
    fetchSourceConfigurations,
    createSourceConf,
    getSourceConfiguration,
    updateSourceConf,
    deleteSourceConf,
    fetchDestinationConfigurations,
    createDestinationConf,
    getDestinationConfiguration,
    updateDestinationConf,
    tryGet,
    tryPost,
    tryPatch,
    tryPut,
    tryDelete,
    tryHead,
    tryOptions,
    tryScenario
  )

  private val allLogic = List[ZServerEndpoint[WLD, Any]](
    fetchStates.zServerLogic(handler.fetchStates),
    testXPath.zServerLogic(req => ZIO.attempt(handler.testXpath(req))),
    fetchServices.zServerLogic(_ => handler.fetchServices),
    createService.zServerLogic(handler.createService),
    getService.zServerLogic(handler.getService),
    fetchStubs.zServerLogic((handler.fetchStubs _).tupled),
    createHttpStub.zServerLogic(handler.createHttpStub),
    getStub.zServerLogic(handler.getStub),
    updateStub.zServerLogic((handler.updateStub _).tupled),
    deleteStub.zServerLogic(handler.deleteStub2),
    fetchScenarios.zServerLogic((handler.fetchScenarios _).tupled),
    createScenario.zServerLogic(handler.createScenario),
    getScenario.zServerLogic(handler.getScenario),
    updateScenario.zServerLogic((handler.updateScenario _).tupled),
    deleteScenario.zServerLogic(handler.deleteScenario2),
    getLabels.zServerLogic(handler.getLabels),
    fetchGrpcStubs.zServerLogic((handler.fetchGrpcStubs _).tupled),
    createGrpcStub.zServerLogic(handler.createGrpcStub),
    getGrpcStub.zServerLogic(handler.getGrpcStub),
    deleteGrpcStub.zServerLogic(handler.deleteGrpcStub),
    fetchSourceConfigurations.zServerLogic(handler.fetchSourceConfigurations),
    createSourceConf.zServerLogic(handler.createSourceConfiguration),
    getSourceConfiguration.zServerLogic(handler.getSourceConfiguration),
    updateSourceConf.zServerLogic((handler.updateSourceConfiguration _).tupled),
    deleteSourceConf.zServerLogic(handler.deleteSourceConfiguration),
    fetchDestinationConfigurations.zServerLogic(handler.fetchDestinationConfigurations _),
    createDestinationConf.zServerLogic(handler.createDestinationConfiguration),
    getDestinationConfiguration.zServerLogic(handler.getDestinationConfiguration),
    updateDestinationConf.zServerLogic((handler.updateDestinationConfiguration _).tupled),
    tryGet.zServerLogic((handler.tryResolveStub(_, _, _, _, AbsentRequestBody)).tupled),
    tryPost.zServerLogic((handler.tryResolveStub _).tupled),
    tryPatch.zServerLogic((handler.tryResolveStub _).tupled),
    tryPut.zServerLogic((handler.tryResolveStub _).tupled),
    tryDelete.zServerLogic((handler.tryResolveStub _).tupled),
    tryHead.zServerLogic((handler.tryResolveStub(_, _, _, _, AbsentRequestBody)).tupled),
    tryOptions.zServerLogic((handler.tryResolveStub(_, _, _, _, AbsentRequestBody)).tupled),
    tryScenario.zServerLogic(handler.tryResolveScenario)
  )

  private val swaggerEndpoints =
    SwaggerInterpreter(
      swaggerUIOptions = SwaggerUIOptions(
        "api" :: "internal" :: "mockingbird" :: "swagger" :: Nil,
        "docs.yaml",
        Nil,
        useRelativePaths = false
      )
    ).fromEndpoints[RIO[WLD, *]](allEndpoints, "Mockingbird", BuildInfo.version)

  private val serverOptions =
    VertxZioServerOptions
      .customiseInterceptors[WLD]
      .options

  val http: List[Router => Route] =
    (allLogic ++ swaggerEndpoints).map(
      VertxZioServerInterpreter(serverOptions).route(_)(wldRuntime)
    )
}

object AdminHttp {
  def live: RLayer[ServerConfig & AdminApiHandler, AdminHttp] =
    ZLayer {
      for {
        sc  <- ZIO.service[ServerConfig]
        aah <- ZIO.service[AdminApiHandler]
      } yield new AdminHttp(sc, aah)
    }
}
