package ru.tinkoff.tcb.mockingbird

import scalapb.zio_grpc.RequestContext
import scalapb.zio_grpc.Server
import scalapb.zio_grpc.ServerLayer
import scalapb.zio_grpc.ServiceList
import scalapb.zio_grpc.server.ZServerCallHandler

import com.mongodb.ConnectionString
import io.grpc.ServerBuilder
import io.grpc.Status
import org.mongodb.scala.MongoClient
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.bson.BsonDocument
import sttp.client3.SttpBackendOptions
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import tofu.logging.Logging
import tofu.logging.impl.ZUniversalLogging
import zio.managed.*

import ru.tinkoff.tcb.mockingbird.api.AdminApiHandler
import ru.tinkoff.tcb.mockingbird.api.AdminHttp
import ru.tinkoff.tcb.mockingbird.api.MetricsHttp
import ru.tinkoff.tcb.mockingbird.api.PublicApiHandler
import ru.tinkoff.tcb.mockingbird.api.PublicHttp
import ru.tinkoff.tcb.mockingbird.api.StubResolver
import ru.tinkoff.tcb.mockingbird.api.Tracing
import ru.tinkoff.tcb.mockingbird.api.UIHttp
import ru.tinkoff.tcb.mockingbird.api.WLD
import ru.tinkoff.tcb.mockingbird.api.WebAPI
import ru.tinkoff.tcb.mockingbird.config.MockingbirdConfiguration
import ru.tinkoff.tcb.mockingbird.config.MongoCollections
import ru.tinkoff.tcb.mockingbird.config.MongoConfig
import ru.tinkoff.tcb.mockingbird.config.ProxyConfig
import ru.tinkoff.tcb.mockingbird.config.ProxyServerType
import ru.tinkoff.tcb.mockingbird.config.ServerConfig
import ru.tinkoff.tcb.mockingbird.dal.*
import ru.tinkoff.tcb.mockingbird.grpc.GrpcRequestHandler
import ru.tinkoff.tcb.mockingbird.grpc.GrpcRequestHandlerImpl
import ru.tinkoff.tcb.mockingbird.grpc.GrpcStubResolverImpl
import ru.tinkoff.tcb.mockingbird.grpc.ProtobufSchemaResolverImpl
import ru.tinkoff.tcb.mockingbird.grpc.UniversalHandlerRegistry
import ru.tinkoff.tcb.mockingbird.resource.ResourceManager
import ru.tinkoff.tcb.mockingbird.scenario.ScenarioEngine
import ru.tinkoff.tcb.mockingbird.scenario.ScenarioResolver
import ru.tinkoff.tcb.mockingbird.stream.EphemeralCleaner
import ru.tinkoff.tcb.mockingbird.stream.EventSpawner
import ru.tinkoff.tcb.mockingbird.stream.SDFetcher
import ru.tinkoff.tcb.utils.metrics.makeRegistry

object Mockingbird extends scala.App {
  type FL = WLD & ServerConfig & PublicHttp & EventSpawner & ResourceManager & EphemeralCleaner & GrpcRequestHandler

  private val zioLog: Logging[UIO] = new ZUniversalLogging(this.getClass.getName)

  private val mongoLayer = ZLayer {
    for {
      config <- ZIO.service[MongoConfig]
    } yield MongoClient(config.uri).getDatabase(new ConnectionString(config.uri).getDatabase)
  }

  private def collection(
      name: MongoCollections => String
  ): URLayer[MongoConfig & MongoDatabase, MongoCollection[BsonDocument]] =
    ZLayer {
      for {
        mongo  <- ZIO.service[MongoDatabase]
        config <- ZIO.service[MongoConfig]
      } yield mongo.getCollection(name(config.collections))
    }

  private def program =
    (for {
      _       <- ZIO.unit.toManagedWith(_ => zioLog.info("Done releasing resources"))
      ec      <- ZIO.service[EphemeralCleaner].toManaged
      _       <- ec.run.forkDaemon.toManaged
      _       <- ResourceManager.managed
      fetcher <- ZIO.service[SDFetcher].toManaged
      _       <- fetcher.run.forkDaemon.toManaged
      spawner <- ZIO.service[EventSpawner].toManaged
      _       <- spawner.run.forkDaemon.toManaged
      _       <- WebAPI.managed
      _ <- zioLog
        .info(s"App started")
        .toManagedWith(_ => zioLog.info("Start releasing resources"))
    } yield ())
      .use(_ => ZIO.never)
      .exitCode

  private val server = ZLayer.fromZIO {
    program
      .provide(
        Tracing.live,
        ZLayer.succeed(makeRegistry("mockingbird")),
        MockingbirdConfiguration.server,
        MockingbirdConfiguration.security,
        MockingbirdConfiguration.mongo,
        MockingbirdConfiguration.proxy,
        MockingbirdConfiguration.event,
        ZLayer.scoped {
          for {
            pc <- ZIO.service[ProxyConfig]
            scopedBackend <- AsyncHttpClientZioBackend.scoped(
              SttpBackendOptions.Default
                .copy(proxy =
                  pc.proxyServer.map(psc =>
                    SttpBackendOptions
                      .Proxy(
                        psc.host,
                        psc.port,
                        (psc.`type`: @unchecked) match {
                          case ProxyServerType.Http  => SttpBackendOptions.ProxyType.Http
                          case ProxyServerType.Socks => SttpBackendOptions.ProxyType.Socks
                        },
                        psc.nonProxy.to(List),
                        psc.auth.map(psa => SttpBackendOptions.ProxyAuth(psa.user, psa.password)),
                        psc.onlyProxy.to(List)
                      )
                  )
                )
            )
          } yield scopedBackend
        },
        mongoLayer,
        aesEncoder,
        collection(_.stub) >>> HttpStubDAOImpl.live,
        collection(_.state) >>> PersistentStateDAOImpl.live,
        collection(_.scenario) >>> ScenarioDAOImpl.live,
        collection(_.service) >>> ServiceDAOImpl.live,
        collection(_.label) >>> LabelDAOImpl.live,
        collection(_.grpcStub) >>> GrpcStubDAOImpl.live,
        collection(_.source) >>> SourceConfigurationDAOImpl.live,
        collection(_.destination) >>> DestinationConfigurationDAOImpl.live,
        ProtobufSchemaResolverImpl.live,
        PublicApiHandler.live,
        PublicHttp.live,
        AdminApiHandler.live,
        AdminHttp.live,
        UIHttp.live,
        MetricsHttp.live,
        EphemeralCleaner.live,
        ScenarioResolver.live,
        ScenarioEngine.live,
        StubResolver.live,
        SDFetcher.live,
        EventSpawner.live,
        ResourceManager.live
      )
  }

  def port: Int = 9000

  def services: ServiceList[Any] = ServiceList.empty

  val builder: ServerBuilder[?] = ServerBuilder.forPort(port)

  def serverLive: ZLayer[Any, Throwable, Server] = ServerLayer.fromServiceList(builder, services)

  val registry: UIO[Unit] = for {
    runtime <- zio.ZIO.runtime[Any]
    handler = ZServerCallHandler.unaryCallHandler(
      runtime,
      (bytes: Array[Byte]) =>
        GrpcRequestHandler
          .exec(bytes)
          .provideSome[RequestContext](
            Tracing.live,
            MockingbirdConfiguration.mongo,
            mongoLayer,
            collection(_.state) >>> PersistentStateDAOImpl.live,
            collection(_.grpcStub) >>> GrpcStubDAOImpl.live,
            GrpcStubResolverImpl.live,
            GrpcRequestHandlerImpl.live
          )
          .mapError(e => Status.INTERNAL.withDescription(e.getMessage))
    )
    mutableRegistry = UniversalHandlerRegistry(
      handler
    )
    _ = builder.fallbackHandlerRegistry(mutableRegistry)
  } yield ()

  Unsafe.unsafe { implicit us =>
    wldRuntime.unsafe.run {
      (registry *> zioLog.info(s"GRPC server started at port: $port") *> ZIO.scoped[Any](
        server.build *> ZIO.never
      ))
        .catchAll(ex => zioLog.errorCause(ex.getMessage, ex))
        .catchAllDefect(ex => zioLog.errorCause(ex.getMessage, ex))
        .exitCode
    }
  }
}
