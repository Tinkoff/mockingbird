package ru.tinkoff.tcb.mockingbird.api

import scala.util.control.NonFatal

import io.circe.Json
import io.circe.parser.parse
import io.scalaland.chimney.dsl.*
import kantan.xpath.*
import kantan.xpath.implicits.*
import org.mongodb.scala.bson.*

import ru.tinkoff.tcb.criteria.*
import ru.tinkoff.tcb.criteria.Typed.*
import ru.tinkoff.tcb.criteria.Untyped.*
import ru.tinkoff.tcb.logging.MDCLogging
import ru.tinkoff.tcb.mockingbird.api.request.*
import ru.tinkoff.tcb.mockingbird.api.response.DestinationDTO
import ru.tinkoff.tcb.mockingbird.api.response.OperationResult
import ru.tinkoff.tcb.mockingbird.api.response.SourceDTO
import ru.tinkoff.tcb.mockingbird.dal.DestinationConfigurationDAO
import ru.tinkoff.tcb.mockingbird.dal.GrpcStubDAO
import ru.tinkoff.tcb.mockingbird.dal.HttpStubDAO
import ru.tinkoff.tcb.mockingbird.dal.LabelDAO
import ru.tinkoff.tcb.mockingbird.dal.PersistentStateDAO
import ru.tinkoff.tcb.mockingbird.dal.ScenarioDAO
import ru.tinkoff.tcb.mockingbird.dal.ServiceDAO
import ru.tinkoff.tcb.mockingbird.dal.SourceConfigurationDAO
import ru.tinkoff.tcb.mockingbird.error.*
import ru.tinkoff.tcb.mockingbird.error.DuplicationError
import ru.tinkoff.tcb.mockingbird.error.ValidationError
import ru.tinkoff.tcb.mockingbird.grpc.ProtobufSchemaResolver
import ru.tinkoff.tcb.mockingbird.model.DestinationConfiguration
import ru.tinkoff.tcb.mockingbird.model.GrpcStub
import ru.tinkoff.tcb.mockingbird.model.HttpMethod
import ru.tinkoff.tcb.mockingbird.model.HttpStub
import ru.tinkoff.tcb.mockingbird.model.Label
import ru.tinkoff.tcb.mockingbird.model.PersistentState
import ru.tinkoff.tcb.mockingbird.model.Scenario
import ru.tinkoff.tcb.mockingbird.model.Scope
import ru.tinkoff.tcb.mockingbird.model.Service
import ru.tinkoff.tcb.mockingbird.model.SourceConfiguration
import ru.tinkoff.tcb.mockingbird.resource.ResourceManager
import ru.tinkoff.tcb.mockingbird.scenario.ScenarioResolver
import ru.tinkoff.tcb.mockingbird.stream.SDFetcher
import ru.tinkoff.tcb.protocol.fields.*
import ru.tinkoff.tcb.protocol.rof.*
import ru.tinkoff.tcb.utils.crypto.AES
import ru.tinkoff.tcb.utils.id.SID
import ru.tinkoff.tcb.utils.xml.*

final class AdminApiHandler(
    stubDAO: HttpStubDAO[Task],
    scenarioDAO: ScenarioDAO[Task],
    stateDAO: PersistentStateDAO[Task],
    serviceDAO: ServiceDAO[Task],
    labelDAO: LabelDAO[Task],
    grpcStubDAO: GrpcStubDAO[Task],
    sourceDAO: SourceConfigurationDAO[Task],
    destinationDAO: DestinationConfigurationDAO[Task],
    fetcher: SDFetcher,
    stubResolver: StubResolver,
    scenarioResolver: ScenarioResolver,
    protobufSchemaResolver: ProtobufSchemaResolver,
    rm: ResourceManager
)(implicit aes: AES) {
  private val log = MDCLogging.`for`[WLD](this)

  def createHttpStub(body: CreateStubRequest): RIO[WLD, OperationResult[SID[HttpStub]]] =
    for {
      service1 <- ZIO.foreach(body.path.map(_.value))(serviceDAO.getServiceFor).map(_.flatten)
      service2 <- ZIO.foreach(body.pathPattern)(serviceDAO.getServiceFor).map(_.flatten)
      _ <- ZIO.when(service1.isEmpty && service2.isEmpty)(
        ZIO.fail(
          ValidationError(
            Vector(s"Не удалось подобрать сервис для ${body.path.orElse(body.pathPattern.map(_.regex)).getOrElse("")}")
          )
        )
      )
      service = service1.orElse(service2).get
      candidates0 <- stubDAO.findChunk(
        prop[HttpStub](_.method) === body.method &&
          (if (body.path.isDefined) prop[HttpStub](_.path) === body.path.map(_.value)
           else prop[HttpStub](_.pathPattern) === body.pathPattern) &&
          prop[HttpStub](_.scope) === body.scope &&
          prop[HttpStub](_.times) > Option(0),
        0,
        Int.MaxValue
      )
      candidates1 = candidates0.filter(_.request == body.request)
      candidates2 = candidates1.filter(_.state == body.state)
      _ <- ZIO.when(candidates2.nonEmpty)(
        ZIO.fail(
          DuplicationError("Существует заглушка(-ки), полностью совпадающая по условиям и типу", candidates2.map(_.id))
        )
      )
      now <- ZIO.clockWith(_.instant)
      stub = body
        .into[HttpStub]
        .withFieldComputed(_.id, _ => SID.random[HttpStub])
        .withFieldConst(_.created, now)
        .withFieldComputed(_.name, _.name.value)
        .withFieldComputed(_.path, _.path.map(_.value))
        .withFieldComputed(_.times, _.times.map(_.value))
        .withFieldConst(_.serviceSuffix, service.suffix)
        .transform
      destinations <- fetcher.getDestinations
      destNames = destinations.map(_.name).toSet
      vr        = HttpStub.validationRules(destNames)(stub)
      _   <- ZIO.when(vr.nonEmpty)(ZIO.fail(ValidationError(vr)))
      res <- stubDAO.insert(stub)
      _   <- labelDAO.ensureLabels(service.suffix, stub.labels.to(Vector))
    } yield if (res > 0) OperationResult("success", stub.id) else OperationResult("nothing inserted")

  def createScenario(body: CreateScenarioRequest): RIO[WLD, OperationResult[SID[Scenario]]] =
    for {
      service <- serviceDAO.findById(body.service)
      _ <- ZIO.when(service.isEmpty)(
        ZIO.fail(
          ValidationError(
            Vector(s"Сервис ${body.service} не существует")
          )
        )
      )
      candidates0 <- scenarioDAO.findChunk(
        prop[Scenario](_.source) === body.source &&
          prop[Scenario](_.scope) === body.scope &&
          prop[HttpStub](_.times) > Option(0),
        0,
        Int.MaxValue
      )
      candidates1 = candidates0.filter(_.input == body.input)
      candidates2 = candidates1.filter(_.state == body.state)
      _ <- ZIO.when(candidates2.nonEmpty)(
        ZIO.fail(
          DuplicationError(
            "Существует сценарий(и), полностью совпадающий по источнику, условиям и типу",
            candidates2.map(_.id)
          )
        )
      )
      now <- ZIO.clockWith(_.instant)
      scenario = body
        .into[Scenario]
        .withFieldComputed(_.id, _ => SID.random[Scenario])
        .withFieldConst(_.created, now)
        .withFieldComputed(_.times, _.times.map(_.value))
        .withFieldComputed(_.name, _.name.value)
        .transform
      sources <- fetcher.getSources
      sourceNames = sources.map(_.name).toSet
      destinations <- fetcher.getDestinations
      destNames = destinations.map(_.name).toSet
      vr        = Scenario.validationRules(sourceNames, destNames)(scenario)
      _   <- ZIO.when(vr.nonEmpty)(ZIO.fail(ValidationError(vr)))
      res <- scenarioDAO.insert(scenario)
      _   <- labelDAO.ensureLabels(service.get.suffix, scenario.labels.to(Vector))
    } yield if (res > 0) OperationResult("success", scenario.id) else OperationResult("nothing inserted")

  def createService(body: CreateServiceRequest): RIO[WLD, OperationResult[String]] =
    for {
      candidates <- serviceDAO.findChunk(prop[Service](_.suffix) === body.suffix.value, 0, Int.MaxValue)
      _ <- ZIO.when(candidates.nonEmpty)(
        ZIO.fail(
          DuplicationError(
            s"Существует сервис(ы), имеющий суффикс ${body.suffix.value}",
            candidates.map(_.name)
          )
        )
      )
      service = body
        .into[Service]
        .withFieldComputed(_.suffix, _.suffix.value)
        .withFieldComputed(_.name, _.name.value)
        .transform
      res <- serviceDAO.insert(service)
    } yield if (res > 0) OperationResult("success", service.suffix) else OperationResult("nothing inserted")

  def fetchStates(body: SearchRequest): RIO[WLD, Vector[PersistentState]] =
    stateDAO.findBySpec(body.query)

  def testXpath(body: XPathTestRequest): String =
    body.xml.toKNode.evalXPath[Node](body.path.toXPathExpr) match {
      case Left(error) => s"Error: $error"
      case Right(node) => s"Success: ${node.print()}"
    }

  def tryResolveStub(
      method: HttpMethod,
      path: String,
      headers: Map[String, String],
      query: Map[String, String],
      body: String
  ): RIO[WLD, SID[HttpStub]] = {
    val queryObject = Json.fromFields(query.view.mapValues(s => parse(s).getOrElse(Json.fromString(s))))
    val f           = stubResolver.findStubAndState(method, path, headers, queryObject, body) _

    for {
      _ <- Tracing.update(_.addToPayload("path" -> path, "method" -> method.entryName))
      (stub, _) <- f(Scope.Countdown)
        .filterOrElse(_.isDefined)(f(Scope.Ephemeral).filterOrElse(_.isDefined)(f(Scope.Persistent)))
        .someOrFail(StubSearchError(s"Не удалось подобрать заглушку для [$method] $path"))
    } yield stub.id
  }

  def tryResolveScenario(body: ScenarioResolveRequest): RIO[WLD, SID[Scenario]] = {
    val f = scenarioResolver.findScenarioAndState(body.source, body.message) _

    for {
      (scenario, _) <- f(Scope.Countdown)
        .filterOrElse(_.isDefined)(f(Scope.Ephemeral).filterOrElse(_.isDefined)(f(Scope.Persistent)))
        .someOrFail(ScenarioSearchError(s"Не удалось подобрать сценарий для сообщения из ${body.source}"))
    } yield scenario.id
  }

  def fetchServices: RIO[WLD, Vector[Service]] =
    serviceDAO.findChunk(Document(), 0, Int.MaxValue)

  def getService(suffix: String): RIO[WLD, Option[Service]] =
    serviceDAO.findById(suffix)

  def fetchStubs(
      page: Option[Int],
      query: Option[String],
      service: Option[String],
      labels: List[String]
  ): RIO[WLD, Vector[HttpStub]] = {
    var queryDoc = prop[HttpStub](_.scope) =/= [Scope] Scope.Countdown || prop[HttpStub](_.times) > Option(0)
    if (query.isDefined) {
      val qs = query.get
      val q = prop[HttpStub](_.id) === [SID[HttpStub]] SID(qs) ||
        prop[HttpStub](_.name).regex(qs, "i") ||
        prop[HttpStub](_.path).regex(qs, "i") ||
        prop[HttpStub](_.pathPattern).regex(qs, "i")
      queryDoc = queryDoc && q
    }
    if (service.isDefined) {
      queryDoc = queryDoc && (prop[HttpStub](_.serviceSuffix) === service.get)
    }
    if (labels.nonEmpty) {
      queryDoc = queryDoc && (prop[HttpStub](_.labels).containsAll(labels))
    }
    stubDAO.findChunk(queryDoc, page.getOrElse(0) * 20, 20, prop[HttpStub](_.created).sort(Desc))
  }

  def fetchScenarios(
      page: Option[Int],
      query: Option[String],
      service: Option[String],
      labels: List[String]
  ): RIO[WLD, Vector[Scenario]] = {
    var queryDoc = prop[Scenario](_.scope) =/= [Scope] Scope.Countdown || prop[Scenario](_.times) > Option(0)
    if (query.isDefined) {
      val qs = query.get
      val q = prop[Scenario](_.id) === [SID[Scenario]] SID(qs) ||
        prop[Scenario](_.name).regex(qs, "i") ||
        prop[Scenario](_.source).regex(qs, "i") ||
        prop[Scenario](_.destination).regex(qs, "i")
      queryDoc = queryDoc && q
    }
    if (service.isDefined) {
      queryDoc = queryDoc && (prop[Scenario](_.service) === service.get)
    }
    if (labels.nonEmpty) {
      queryDoc = queryDoc && (prop[Scenario](_.labels).containsAll(labels))
    }
    scenarioDAO.findChunk(queryDoc, page.getOrElse(0) * 20, 20, prop[HttpStub](_.created).sort(Desc))
  }

  def getStub(id: SID[HttpStub]): RIO[WLD, Option[HttpStub]] =
    stubDAO.findById(id)

  def getScenario(id: SID[Scenario]): RIO[WLD, Option[Scenario]] =
    scenarioDAO.findById(id)

  def deleteStub2(id: SID[HttpStub]): RIO[WLD, OperationResult[String]] =
    ZIO.ifZIO(stubDAO.deleteById(id).map(_ > 0))(
      ZIO.succeed(OperationResult("success")),
      ZIO.succeed(OperationResult("nothing deleted"))
    )

  def deleteScenario2(id: SID[Scenario]): RIO[WLD, OperationResult[String]] =
    ZIO.ifZIO(scenarioDAO.deleteById(id).map(_ > 0))(
      ZIO.succeed(OperationResult("success")),
      ZIO.succeed(OperationResult("nothing deleted"))
    )

  def updateStub(id: SID[HttpStub], body: UpdateStubRequest): RIO[WLD, OperationResult[SID[HttpStub]]] =
    for {
      service1 <- ZIO.foreach(body.path.map(_.value))(serviceDAO.getServiceFor).map(_.flatten)
      service2 <- ZIO.foreach(body.pathPattern)(serviceDAO.getServiceFor).map(_.flatten)
      _ <- ZIO.when(service1.isEmpty && service2.isEmpty)(
        ZIO.fail(
          ValidationError(
            Vector(s"Не удалось подобрать сервис для ${body.path.orElse(body.pathPattern.map(_.regex)).getOrElse("")}")
          )
        )
      )
      service = service1.orElse(service2).get
      candidates0 <- stubDAO.findChunk(
        where(_._id =/= id) &&
          prop[HttpStub](_.method) === body.method &&
          (if (body.path.isDefined) prop[HttpStub](_.path) === body.path.map(_.value)
           else prop[HttpStub](_.pathPattern) === body.pathPattern) &&
          prop[HttpStub](_.scope) === body.scope &&
          prop[HttpStub](_.times) > Option(0),
        0,
        Int.MaxValue
      )
      candidates1 = candidates0.filter(_.request == body.request)
      candidates2 = candidates1.filter(_.state == body.state)
      _ <- ZIO.when(candidates2.nonEmpty)(
        ZIO.fail(
          DuplicationError("Существует заглушка(-ки), полностью совпадающая по условиям и типу", candidates2.map(_.id))
        )
      )
      now <- ZIO.clockWith(_.instant)
      stubPatch = body
        .into[StubPatch]
        .withFieldConst(_.id, id)
        .withFieldComputed(_.times, _.times.map(_.value))
        .withFieldComputed(_.name, _.name.value)
        .withFieldComputed(_.path, _.path.map(_.value))
        .transform
      stub = stubPatch
        .into[HttpStub]
        .withFieldConst(_.created, now)
        .withFieldConst(_.serviceSuffix, service.suffix)
        .transform
      destinations <- fetcher.getDestinations
      destNames = destinations.map(_.name).toSet
      vr        = HttpStub.validationRules(destNames)(stub)
      _   <- ZIO.when(vr.nonEmpty)(ZIO.fail(ValidationError(vr)))
      res <- stubDAO.patch(stubPatch)
      _   <- labelDAO.ensureLabels(service.suffix, stubPatch.labels.to(Vector))
    } yield if (res.successful) OperationResult("success", stub.id) else OperationResult("nothing updated")

  def updateScenario(id: SID[Scenario], body: UpdateScenarioRequest): RIO[WLD, OperationResult[SID[Scenario]]] =
    for {
      service <- serviceDAO.findById(body.service)
      _ <- ZIO.when(service.isEmpty)(
        ZIO.fail(
          ValidationError(
            Vector(s"Сервис ${body.service} не существует")
          )
        )
      )
      candidates0 <- scenarioDAO.findChunk(
        where(_._id =/= id) &&
          prop[Scenario](_.source) === body.source &&
          prop[Scenario](_.scope) === body.scope &&
          prop[HttpStub](_.times) > Option(0),
        0,
        Int.MaxValue
      )
      candidates1 = candidates0.filter(_.input == body.input)
      candidates2 = candidates1.filter(_.state == body.state)
      _ <- ZIO.when(candidates2.nonEmpty)(
        ZIO.fail(
          DuplicationError(
            "Существует сценарий(и), полностью совпадающий по источнику, условиям и типу",
            candidates2.map(_.id)
          )
        )
      )
      now <- ZIO.clockWith(_.instant)
      scenarioPatch = body
        .into[ScenarioPatch]
        .withFieldConst(_.id, id)
        .withFieldComputed(_.times, _.times.map(_.value))
        .withFieldComputed(_.name, _.name.value)
        .transform
      scenario = scenarioPatch
        .into[Scenario]
        .withFieldConst(_.created, now)
        .transform
      sources <- fetcher.getSources
      sourceNames = sources.map(_.name).toSet
      destinations <- fetcher.getDestinations
      destNames = destinations.map(_.name).toSet
      vr        = Scenario.validationRules(sourceNames, destNames)(scenario)
      _   <- ZIO.when(vr.nonEmpty)(ZIO.fail(ValidationError(vr)))
      res <- scenarioDAO.patch(scenarioPatch)
      _   <- labelDAO.ensureLabels(body.service, scenario.labels.to(Vector))
    } yield if (res.successful) OperationResult("success", scenario.id) else OperationResult("nothing inserted")

  def getLabels(service: String): RIO[WLD, Vector[String]] =
    labelDAO.findChunk(prop[Label](_.serviceSuffix) === service, 0, Int.MaxValue).map(_.map(_.label))

  def fetchGrpcStubs(
      page: Option[Int],
      query: Option[String],
      service: Option[String],
      labels: List[String]
  ): RIO[WLD, Vector[GrpcStub]] = {
    var queryDoc = prop[GrpcStub](_.scope) =/= [Scope] Scope.Countdown || prop[GrpcStub](_.times) > Option(0)
    if (query.isDefined) {
      val qs = query.get
      val q = prop[GrpcStub](_.id) === [SID[GrpcStub]] SID(qs) ||
        prop[GrpcStub](_.name).regex(qs, "i") ||
        prop[GrpcStub](_.methodName).regex(qs, "i")
      queryDoc = queryDoc && q
    }
    if (service.isDefined) {
      queryDoc = queryDoc && (prop[GrpcStub](_.service) === service.get)
    }
    if (labels.nonEmpty) {
      queryDoc = queryDoc && (prop[GrpcStub](_.labels).containsAll(labels))
    }
    grpcStubDAO.findChunk(queryDoc, page.getOrElse(0) * 20, 20, prop[GrpcStub](_.created).sort(Desc))
  }

  def createGrpcStub(body: CreateGrpcStubRequest): RIO[WLD, OperationResult[SID[GrpcStub]]] = {
    val requestSchemaBytes  = body.requestCodecs.asArray
    val responseSchemaBytes = body.responseCodecs.asArray
    for {
      service <- serviceDAO.findById(body.service)
      _ <- ZIO.when(service.isEmpty)(
        ZIO.fail(
          ValidationError(
            Vector(s"Сервис ${body.service} не существует")
          )
        )
      )
      requestSchema <- protobufSchemaResolver.parseDefinitionFrom(requestSchemaBytes)
      _ <- ZIO.foreachParDiscard(body.requestPredicates.definition.keys)(
        GrpcStub.validateOptics(_, body.requestClass, requestSchema)
      )
      candidates0 <- grpcStubDAO.findChunk(
        prop[GrpcStub](_.methodName) === body.methodName,
        0,
        Integer.MAX_VALUE
      )
      candidates = candidates0
        .filter(_.requestClass == body.requestClass)
        .filter(_.requestSchema == requestSchema)
        .filter(_.requestPredicates.definition == body.state)
      _ <- ZIO.when(candidates.nonEmpty)(
        ZIO.fail(
          DuplicationError("Существует заглушка(-ки), полностью совпадающая по условиям и типу", candidates.map(_.id))
        )
      )
      responseSchema <- protobufSchemaResolver.parseDefinitionFrom(responseSchemaBytes)
      now            <- ZIO.clockWith(_.instant)
      stub = body
        .into[GrpcStub]
        .withFieldConst(_.created, now)
        .withFieldComputed(_.times, _.times.map(_.value))
        .withFieldComputed(_.requestSchema, _ => requestSchema)
        .withFieldComputed(_.responseSchema, _ => responseSchema)
        .transform
      vr = GrpcStub.validationRules(stub)
      _   <- ZIO.when(vr.nonEmpty)(ZIO.fail(ValidationError(vr)))
      res <- grpcStubDAO.insert(stub)
      _   <- labelDAO.ensureLabels(stub.service, stub.labels.to(Vector))
    } yield if (res > 0) OperationResult("success", stub.id) else OperationResult("nothing inserted")
  }

  def getGrpcStub(id: SID[GrpcStub]): RIO[WLD, Option[GrpcStub]] =
    grpcStubDAO.findById(id)

  def deleteGrpcStub(id: SID[GrpcStub]): RIO[WLD, OperationResult[String]] =
    ZIO.ifZIO(grpcStubDAO.deleteById(id).map(_ > 0))(
      ZIO.succeed(OperationResult("success")),
      ZIO.succeed(OperationResult("nothing deleted"))
    )

  def fetchSourceConfigurations(
      service: Option[String]
  ): RIO[WLD, Vector[SourceDTO]] = {
    var queryDoc = BsonDocument()
    if (service.isDefined) {
      queryDoc = prop[Scenario](_.service) === service.get
    }
    sourceDAO
      .findChunkProjection[SourceDTO](queryDoc, 0, Int.MaxValue, prop[SourceConfiguration](_.created).sort(Desc) :: Nil)
  }

  def getSourceConfiguration(name: SID[SourceConfiguration]): RIO[WLD, Option[SourceConfiguration]] =
    sourceDAO.findById(name)

  def createSourceConfiguration(
      body: CreateSourceConfigurationRequest
  ): RIO[WLD, OperationResult[SID[SourceConfiguration]]] =
    for {
      service <- serviceDAO.findById(body.service)
      _ <- ZIO.when(service.isEmpty)(
        ZIO.fail(
          ValidationError(
            Vector(s"Сервис ${body.service} не существует")
          )
        )
      )
      candidate <- sourceDAO.findById(body.name)
      _ <- ZIO.when(candidate.nonEmpty)(
        ZIO.fail(
          DuplicationError(
            "Существует конфигурация, полностью совпадающая по имени",
            candidate.map(_.name).toVector
          )
        )
      )
      now <- ZIO.clockWith(_.instant)
      sourceConf = body
        .into[SourceConfiguration]
        .withFieldConst(_.created, now)
        .transform
      res <- sourceDAO.insert(sourceConf)
      _ <- ZIO
        .foreachDiscard(sourceConf.init.map(_.toVector).getOrElse(Vector.empty))(rm.execute)
        .catchSomeDefect { case NonFatal(ex) =>
          ZIO.fail(ex)
        }
        .catchSome { case NonFatal(ex) =>
          log.errorCause("Ошибка при инициализации", ex)
        }
        .forkDaemon
    } yield if (res > 0) OperationResult("success", sourceConf.name) else OperationResult("nothing inserted")

  def updateSourceConfiguration(
      name: SID[SourceConfiguration],
      body: UpdateSourceConfigurationRequest
  ): RIO[WLD, OperationResult[SID[SourceConfiguration]]] =
    for {
      service <- serviceDAO.findById(body.service)
      _ <- ZIO.when(service.isEmpty)(
        ZIO.fail(
          ValidationError(
            Vector(s"Сервис ${body.service} не существует")
          )
        )
      )
      now <- ZIO.clockWith(_.instant)
      confPatch = body
        .into[SourceConfiguration]
        .withFieldConst(_.name, name)
        .withFieldConst(_.created, now)
        .transform
      res <- sourceDAO.patch(confPatch)
      _ <- (ZIO
        .foreachDiscard(confPatch.shutdown.map(_.toVector).getOrElse(Vector.empty))(rm.execute)
        .catchSomeDefect { case NonFatal(ex) =>
          ZIO.fail(ex)
        }
        .catchSome { case NonFatal(ex) =>
          log.errorCause("Ошибка при деинициализации", ex)
        } *> ZIO
        .foreachDiscard(confPatch.init.map(_.toVector).getOrElse(Vector.empty))(rm.execute)
        .catchSomeDefect { case NonFatal(ex) =>
          ZIO.fail(ex)
        }
        .catchSome { case NonFatal(ex) =>
          log.errorCause("Ошибка при инициализации", ex)
        }).forkDaemon
    } yield if (res.successful) OperationResult("success", confPatch.name) else OperationResult("nothing changed")

  def deleteSourceConfiguration(name: SID[SourceConfiguration]): RIO[WLD, OperationResult[String]] =
    for {
      scenarios <- scenarioDAO.findChunk(
        prop[Scenario](_.source) === name,
        0,
        Int.MaxValue
      )
      _ <- ZIO.when(scenarios.nonEmpty)(
        ZIO.fail(
          ValidationError(
            Vector(s"Сценарии ${scenarios.mkString(",")} используют источник ${name}")
          )
        )
      )
      source <- sourceDAO.findById(name)
      _ <- ZIO.when(source.isEmpty)(
        ZIO.fail(
          ValidationError(
            Vector(s"Конфигурация $name не существует")
          )
        )
      )
      _ <- sourceDAO.deleteById(name) // TODO: удалять в транзакции
      _ <- ZIO
        .foreachDiscard(source.get.shutdown.map(_.toVector).getOrElse(Vector.empty))(rm.execute)
        .catchSomeDefect { case NonFatal(ex) =>
          ZIO.fail(ex)
        }
        .catchSome { case NonFatal(ex) =>
          log.errorCause("Ошибка при деинициализации", ex)
        }
        .forkDaemon
    } yield OperationResult("success", None)

  def fetchDestinationConfigurations(
      service: Option[String]
  ): RIO[WLD, Vector[DestinationDTO]] = {
    var queryDoc = BsonDocument()
    if (service.isDefined) {
      queryDoc = prop[Scenario](_.service) === service.get
    }
    destinationDAO.findChunkProjection[DestinationDTO](
      queryDoc,
      0,
      Int.MaxValue,
      prop[DestinationConfiguration](_.created).sort(Desc) :: Nil
    )
  }

  def getDestinationConfiguration(name: SID[DestinationConfiguration]): RIO[WLD, Option[DestinationConfiguration]] =
    destinationDAO.findById(name)

  def createDestinationConfiguration(
      body: CreateDestinationConfigurationRequest
  ): RIO[WLD, OperationResult[SID[DestinationConfiguration]]] =
    for {
      service <- serviceDAO.findById(body.service)
      _ <- ZIO.when(service.isEmpty)(
        ZIO.fail(
          ValidationError(
            Vector(s"Сервис ${body.service} не существует")
          )
        )
      )
      candidate <- destinationDAO.findById(body.name)
      _ <- ZIO.when(candidate.nonEmpty)(
        ZIO.fail(
          DuplicationError(
            "Существует конфигурация, полностью совпадающая по имени",
            candidate.map(_.name).toVector
          )
        )
      )
      now <- ZIO.clockWith(_.instant)
      destinationConf = body
        .into[DestinationConfiguration]
        .withFieldConst(_.created, now)
        .transform
      res <- destinationDAO.insert(destinationConf)
      _ <- ZIO
        .foreachDiscard(destinationConf.init.map(_.toVector).getOrElse(Vector.empty))(rm.execute)
        .catchSomeDefect { case NonFatal(ex) =>
          ZIO.fail(ex)
        }
        .catchSome { case NonFatal(ex) =>
          log.errorCause("Ошибка при инициализации", ex)
        }
        .forkDaemon
    } yield if (res > 0) OperationResult("success", destinationConf.name) else OperationResult("nothing inserted")

  def updateDestinationConfiguration(
      name: SID[DestinationConfiguration],
      body: UpdateDestinationConfigurationRequest
  ): RIO[WLD, OperationResult[SID[DestinationConfiguration]]] =
    for {
      service <- serviceDAO.findById(body.service)
      _ <- ZIO.when(service.isEmpty)(
        ZIO.fail(
          ValidationError(
            Vector(s"Сервис ${body.service} не существует")
          )
        )
      )
      now <- ZIO.clockWith(_.instant)
      confPatch = body
        .into[DestinationConfiguration]
        .withFieldConst(_.name, name)
        .withFieldConst(_.created, now)
        .transform
      res <- destinationDAO.patch(confPatch)
      _ <- (ZIO
        .foreachDiscard(confPatch.shutdown.map(_.toVector).getOrElse(Vector.empty))(rm.execute)
        .catchSomeDefect { case NonFatal(ex) =>
          ZIO.fail(ex)
        }
        .catchSome { case NonFatal(ex) =>
          log.errorCause("Ошибка при деинициализации", ex)
        } *> ZIO
        .foreachDiscard(confPatch.init.map(_.toVector).getOrElse(Vector.empty))(rm.execute)
        .catchSomeDefect { case NonFatal(ex) =>
          ZIO.fail(ex)
        }
        .catchSome { case NonFatal(ex) =>
          log.errorCause("Ошибка при инициализации", ex)
        }).forkDaemon
    } yield if (res.successful) OperationResult("success", confPatch.name) else OperationResult("nothing changed")
}

object AdminApiHandler {
  val live = ZLayer {
    for {
      hsd              <- ZIO.service[HttpStubDAO[Task]]
      sd               <- ZIO.service[ScenarioDAO[Task]]
      std              <- ZIO.service[PersistentStateDAO[Task]]
      srd              <- ZIO.service[ServiceDAO[Task]]
      ld               <- ZIO.service[LabelDAO[Task]]
      gsd              <- ZIO.service[GrpcStubDAO[Task]]
      srcd             <- ZIO.service[SourceConfigurationDAO[Task]]
      dstd             <- ZIO.service[DestinationConfigurationDAO[Task]]
      ftch             <- ZIO.service[SDFetcher]
      stubResolver     <- ZIO.service[StubResolver]
      scenarioResolver <- ZIO.service[ScenarioResolver]
      protoResolver    <- ZIO.service[ProtobufSchemaResolver]
      aes              <- ZIO.service[AES]
      rm               <- ZIO.service[ResourceManager]
    } yield new AdminApiHandler(
      hsd,
      sd,
      std,
      srd,
      ld,
      gsd,
      srcd,
      dstd,
      ftch,
      stubResolver,
      scenarioResolver,
      protoResolver,
      rm
    )(aes)
  }
}
