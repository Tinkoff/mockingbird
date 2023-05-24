package ru.tinkoff.tcb.mockingbird.api

import sttp.tapir.*
import sttp.tapir.json.circe.*

import ru.tinkoff.tcb.mockingbird.api.input.*
import ru.tinkoff.tcb.mockingbird.api.request.CreateDestinationConfigurationRequest
import ru.tinkoff.tcb.mockingbird.api.request.CreateGrpcStubRequest
import ru.tinkoff.tcb.mockingbird.api.request.CreateScenarioRequest
import ru.tinkoff.tcb.mockingbird.api.request.CreateServiceRequest
import ru.tinkoff.tcb.mockingbird.api.request.CreateSourceConfigurationRequest
import ru.tinkoff.tcb.mockingbird.api.request.CreateStubRequest
import ru.tinkoff.tcb.mockingbird.api.request.ScenarioResolveRequest
import ru.tinkoff.tcb.mockingbird.api.request.SearchRequest
import ru.tinkoff.tcb.mockingbird.api.request.UpdateDestinationConfigurationRequest
import ru.tinkoff.tcb.mockingbird.api.request.UpdateScenarioRequest
import ru.tinkoff.tcb.mockingbird.api.request.UpdateSourceConfigurationRequest
import ru.tinkoff.tcb.mockingbird.api.request.UpdateStubRequest
import ru.tinkoff.tcb.mockingbird.api.request.XPathTestRequest
import ru.tinkoff.tcb.mockingbird.api.response.DestinationDTO
import ru.tinkoff.tcb.mockingbird.api.response.OperationResult
import ru.tinkoff.tcb.mockingbird.api.response.SourceDTO
import ru.tinkoff.tcb.mockingbird.codec.*
import ru.tinkoff.tcb.mockingbird.model.AbsentRequestBody
import ru.tinkoff.tcb.mockingbird.model.DestinationConfiguration
import ru.tinkoff.tcb.mockingbird.model.GrpcStub
import ru.tinkoff.tcb.mockingbird.model.HttpStub
import ru.tinkoff.tcb.mockingbird.model.PersistentState
import ru.tinkoff.tcb.mockingbird.model.RequestBody
import ru.tinkoff.tcb.mockingbird.model.Scenario
import ru.tinkoff.tcb.mockingbird.model.Service
import ru.tinkoff.tcb.mockingbird.model.SimpleRequestBody
import ru.tinkoff.tcb.mockingbird.model.SourceConfiguration
import ru.tinkoff.tcb.utils.id.SID

package object admin {
  private val basic =
    endpoint
      .in("api" / "internal" / "mockingbird")
      .errorOut(plainBody[Throwable])

  private val basicTest = basic.tag("test")

  val fetchStates: Endpoint[Unit, SearchRequest, Throwable, Vector[PersistentState], Any] =
    basicTest.post
      .in("fetchStates")
      .in(jsonBody[SearchRequest])
      .out(jsonBody[Vector[PersistentState]])
      .summary("Выборка состояний по предикату")

  val testXPath: Endpoint[Unit, XPathTestRequest, Throwable, String, Any] =
    basicTest.post
      .in("testXpath")
      .in(jsonBody[XPathTestRequest])
      .out(stringBody)
      .summary("Тестирование работоспособности XPath-выражения")

  private val tryStub: PublicEndpoint[ExecInputB, Throwable, SID[
    HttpStub
  ], Any] =
    basicTest
      .in("tryStub")
      .summary("Проверка резолвинга HTTP заглушки")
      .in(execInput)
      .in(
        binaryBody(RawBodyType.ByteArrayBody)[Option[String]]
          .map[RequestBody]((_: Option[String]).fold[RequestBody](AbsentRequestBody)(SimpleRequestBody(_)))(
            SimpleRequestBody.subset.getOption(_).map(_.value)
          )
      )
      .out(jsonBody[SID[HttpStub]])

  val tryGet: PublicEndpoint[ExecInputB, Throwable, SID[HttpStub], Any] =
    tryStub.get
  val tryPost: PublicEndpoint[ExecInputB, Throwable, SID[HttpStub], Any] =
    tryStub.post
  val tryPatch: PublicEndpoint[ExecInputB, Throwable, SID[HttpStub], Any] =
    tryStub.patch
  val tryPut: PublicEndpoint[ExecInputB, Throwable, SID[HttpStub], Any] =
    tryStub.put
  val tryDelete: PublicEndpoint[ExecInputB, Throwable, SID[HttpStub], Any] =
    tryStub.delete
  val tryHead: PublicEndpoint[ExecInputB, Throwable, SID[HttpStub], Any] =
    tryStub.head
  val tryOptions: PublicEndpoint[ExecInputB, Throwable, SID[HttpStub], Any] =
    tryStub.options

  val tryScenario: PublicEndpoint[ScenarioResolveRequest, Throwable, SID[Scenario], Any] =
    basicTest.post
      .in("tryScenario")
      .in(jsonBody[ScenarioResolveRequest])
      .out(jsonBody[SID[Scenario]])
      .summary("Проверка резолвинга сценария")

  private val basicV2 = basic.in("v2").tag("setup v2")

  private val serviceBase = basicV2.in("service")

  val fetchServices: Endpoint[Unit, Unit, Throwable, Vector[Service], Any] =
    serviceBase.get
      .out(jsonBody[Vector[Service]])
      .summary("Получение списка сервисов")

  val createService: Endpoint[Unit, CreateServiceRequest, Throwable, OperationResult[String], Any] =
    serviceBase.post
      .in(jsonBody[CreateServiceRequest])
      .out(jsonBody[OperationResult[String]])
      .summary("Создание сервиса")

  val getService: Endpoint[Unit, String, Throwable, Option[Service], Any] =
    serviceBase.get
      .in(path[String].name("suffix"))
      .out(jsonBody[Option[Service]])
      .summary("Получение сервиса по суффиксу")

  private val stubBase = basicV2.in("stub")

  val fetchStubs
      : Endpoint[Unit, (Option[Int], Option[String], Option[String], List[String]), Throwable, Vector[HttpStub], Any] =
    stubBase.get
      .in(query[Option[Int]]("page"))
      .in(query[Option[String]]("query"))
      .in(query[Option[String]]("service"))
      .in(query[List[String]]("labels"))
      .out(jsonBody[Vector[HttpStub]])
      .summary("Получение списка заглушек")

  val createHttpStub: Endpoint[Unit, CreateStubRequest, Throwable, OperationResult[SID[HttpStub]], Any] =
    stubBase.post
      .in(jsonBody[CreateStubRequest])
      .out(jsonBody[OperationResult[SID[HttpStub]]])
      .summary("Создание HTTP мока")

  val getStub: Endpoint[Unit, SID[HttpStub], Throwable, Option[HttpStub], Any] =
    stubBase.get
      .in(path[SID[HttpStub]].name("id"))
      .out(jsonBody[Option[HttpStub]])
      .summary("Получение заглушки по id")

  val updateStub: Endpoint[Unit, (SID[HttpStub], UpdateStubRequest), Throwable, OperationResult[SID[HttpStub]], Any] =
    stubBase.patch
      .in(path[SID[HttpStub]].name("id"))
      .in(jsonBody[UpdateStubRequest])
      .out(jsonBody[OperationResult[SID[HttpStub]]])
      .summary("Обновление заглушки по id")

  val deleteStub: Endpoint[Unit, SID[HttpStub], Throwable, OperationResult[String], Any] =
    stubBase.delete
      .in(path[SID[HttpStub]].name("id"))
      .out(jsonBody[OperationResult[String]])
      .summary("Удаление HTTP мока")

  private val scenarioBase = basicV2.in("scenario")

  val fetchScenarios
      : Endpoint[Unit, (Option[Int], Option[String], Option[String], List[String]), Throwable, Vector[Scenario], Any] =
    scenarioBase.get
      .in(query[Option[Int]]("page"))
      .in(query[Option[String]]("query"))
      .in(query[Option[String]]("service"))
      .in(query[List[String]]("labels"))
      .out(jsonBody[Vector[Scenario]])
      .summary("Получение списка сценариев")

  val createScenario: Endpoint[Unit, CreateScenarioRequest, Throwable, OperationResult[SID[Scenario]], Any] =
    scenarioBase.post
      .in(jsonBody[CreateScenarioRequest])
      .out(jsonBody[OperationResult[SID[Scenario]]])
      .summary("Создание событийного мока")

  val getScenario: Endpoint[Unit, SID[Scenario], Throwable, Option[Scenario], Any] =
    scenarioBase.get
      .in(path[SID[Scenario]].name("id"))
      .out(jsonBody[Option[Scenario]])
      .summary("Получение сценария по id")

  val updateScenario
      : Endpoint[Unit, (SID[Scenario], UpdateScenarioRequest), Throwable, OperationResult[SID[Scenario]], Any] =
    scenarioBase.patch
      .in(path[SID[Scenario]].name("id"))
      .in(jsonBody[UpdateScenarioRequest])
      .out(jsonBody[OperationResult[SID[Scenario]]])
      .summary("Обновление сценария по id")

  val deleteScenario: Endpoint[Unit, SID[Scenario], Throwable, OperationResult[String], Any] =
    scenarioBase.delete
      .in(path[SID[Scenario]].name("id"))
      .out(jsonBody[OperationResult[String]])
      .summary("Удаление сценария")

  private val labelBase = basicV2.in("label")

  val getLabels: Endpoint[Unit, String, Throwable, Vector[String], Any] =
    labelBase.get
      .in(query[String]("service"))
      .out(jsonBody[Vector[String]])

  private val grpcStubBase = basicV2.in("grpcStub")

  val fetchGrpcStubs
      : Endpoint[Unit, (Option[Int], Option[String], Option[String], List[String]), Throwable, Vector[GrpcStub], Any] =
    grpcStubBase.get
      .in(query[Option[Int]]("page"))
      .in(query[Option[String]]("query"))
      .in(query[Option[String]]("service"))
      .in(query[List[String]]("labels"))
      .out(jsonBody[Vector[GrpcStub]])

  val createGrpcStub: Endpoint[Unit, CreateGrpcStubRequest, Throwable, OperationResult[SID[GrpcStub]], Any] =
    grpcStubBase.post
      .in(jsonBody[CreateGrpcStubRequest])
      .out(jsonBody[OperationResult[SID[GrpcStub]]])

  val getGrpcStub: Endpoint[Unit, SID[GrpcStub], Throwable, Option[GrpcStub], Any] =
    grpcStubBase.get
      .in(path[SID[GrpcStub]].name("id"))
      .out(jsonBody[Option[GrpcStub]])

  val deleteGrpcStub: Endpoint[Unit, SID[GrpcStub], Throwable, OperationResult[String], Any] =
    grpcStubBase.delete
      .in(path[SID[GrpcStub]].name("id"))
      .out(jsonBody[OperationResult[String]])

  private val basicV3 = basic.in("v3").tag("setup v3")

  private val sourceConfBase = basicV3.in("source")

  val fetchSourceConfigurations: Endpoint[Unit, Option[String], Throwable, Vector[SourceDTO], Any] =
    sourceConfBase.get
      .in(query[Option[String]]("service"))
      .out(jsonBody[Vector[SourceDTO]])
      .summary("Получение списка конфигураций источников")

  val getSourceConfiguration =
    sourceConfBase.get
      .in(path[SID[SourceConfiguration]].name("name"))
      .out(jsonBody[Option[SourceConfiguration]])
      .summary("Получение конфигурации источника по имени")

  val createSourceConf
      : Endpoint[Unit, CreateSourceConfigurationRequest, Throwable, OperationResult[SID[SourceConfiguration]], Any] =
    sourceConfBase.post
      .in(jsonBody[CreateSourceConfigurationRequest])
      .out(jsonBody[OperationResult[SID[SourceConfiguration]]])
      .summary("Создание конфигурации источника")

  val updateSourceConf
      : Endpoint[Unit, (SID[SourceConfiguration], UpdateSourceConfigurationRequest), Throwable, OperationResult[
        SID[SourceConfiguration]
      ], Any] =
    sourceConfBase.patch
      .in(path[SID[SourceConfiguration]].name("name"))
      .in(jsonBody[UpdateSourceConfigurationRequest])
      .out(jsonBody[OperationResult[SID[SourceConfiguration]]])
      .summary("Обновление конфигурации по name")

  val deleteSourceConf: Endpoint[Unit, SID[SourceConfiguration], Throwable, OperationResult[String], Any] =
    sourceConfBase.delete
      .in(path[SID[SourceConfiguration]].name("name"))
      .out(jsonBody[OperationResult[String]])
      .summary("Удаление конфигурации")

  private val destinationConfBase = basicV3.in("destination")

  val fetchDestinationConfigurations: Endpoint[Unit, Option[String], Throwable, Vector[DestinationDTO], Any] =
    destinationConfBase.get
      .in(query[Option[String]]("service"))
      .out(jsonBody[Vector[DestinationDTO]])
      .summary("Получение списка конфигураций назначений")

  val getDestinationConfiguration =
    destinationConfBase.get
      .in(path[SID[DestinationConfiguration]].name("name"))
      .out(jsonBody[Option[DestinationConfiguration]])
      .summary("Получение конфигурации назначения по имени")

  val createDestinationConf: Endpoint[Unit, CreateDestinationConfigurationRequest, Throwable, OperationResult[
    SID[DestinationConfiguration]
  ], Any] =
    destinationConfBase.post
      .in(jsonBody[CreateDestinationConfigurationRequest])
      .out(jsonBody[OperationResult[SID[DestinationConfiguration]]])
      .summary("Создание конфигурации источника")

  val updateDestinationConf: Endpoint[
    Unit,
    (SID[DestinationConfiguration], UpdateDestinationConfigurationRequest),
    Throwable,
    OperationResult[
      SID[DestinationConfiguration]
    ],
    Any
  ] =
    destinationConfBase.patch
      .in(path[SID[DestinationConfiguration]].name("name"))
      .in(jsonBody[UpdateDestinationConfigurationRequest])
      .out(jsonBody[OperationResult[SID[DestinationConfiguration]]])
      .summary("Обновление конфигурации по name")
}
