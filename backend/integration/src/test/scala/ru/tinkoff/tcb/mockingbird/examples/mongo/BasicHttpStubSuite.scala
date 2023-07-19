package ru.tinkoff.tcb.mockingbird.examples.mongo

import java.net.ServerSocket
import scala.concurrent.Await
import scala.util.Using

import com.dimafeng.testcontainers.ContainerDef
import com.dimafeng.testcontainers.GenericContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.client3.*
import sttp.client3.httpclient.zio.HttpClientZioBackend
import sttp.model.Uri
import zio.Exit.Failure
import zio.Exit.Success

import ru.tinkoff.tcb.mockingbird.Mockingbird
import ru.tinkoff.tcb.mockingbird.config.MockingbirdConfiguration
import ru.tinkoff.tcb.mockingbird.config.MongoConfig
import ru.tinkoff.tcb.mockingbird.config.SecurityConfig
import ru.tinkoff.tcb.mockingbird.config.ServerConfig
import ru.tinkoff.tcb.mockingbird.edsl.interpreter.AsyncScalaTestSuite
import ru.tinkoff.tcb.mockingbird.examples

class BasicHttpStubSuite extends AsyncScalaTestSuite with TestContainerForAll {
  import HttpStubSuite.*

  private val set = new examples.BasicHttpStub[HttpResponseR]()

  var httpPort                                       = 0
  val mongoExposedPort                               = 27017
  var mockingbird: Option[CancelableFuture[Nothing]] = None

  override def baseUri: Uri = uri"http://localhost:$httpPort"

  override val containerDef: ContainerDef = GenericContainer.Def(
    dockerImage = "mongo",
    exposedPorts = Seq(mongoExposedPort),
  )

  override def afterContainersStart(containers: Containers): Unit = {
    super.afterContainersStart(containers)
    val c                = containers.asInstanceOf[GenericContainer]
    val connectionString = s"mongodb://${c.containerIpAddress}:${c.mappedPort(mongoExposedPort)}/mockingbird-db"

    Using.Manager { use =>
      val ss1 = use(new ServerSocket(0))

      httpPort = ss1.getLocalPort()
    }.get

    val server = Unsafe.unsafe { implicit us =>
      val cfg = MockingbirdConfiguration.live
        .update[MongoConfig](_.copy(uri = connectionString))
        .update[ServerConfig](_.copy(port = httpPort, healthCheckRoute = "/ready".some, allowedOrigins = Seq("*")))
        .update[SecurityConfig](_.copy(secret = "secret"))
      Runtime.default.unsafe.runToFuture(Mockingbird.app(cfg))
    }

    mockingbird = server.some

    val e = Unsafe.unsafe { implicit unsafe =>
      Runtime.default.unsafe.run(
        (for {
          _ <- waitForReadiness(baseUri, server)
          r <- initTestPrerequisites(baseUri)
        } yield r).provide(HttpClientZioBackend.layer())
      )
    }

    e match {
      case Success(Response(Right(_), _, _, _, _, _))      => info("Mockingbird server started")
      case Success(Response(Left(body), code, _, _, _, _)) => fail(s"service creating failed, code $code: $body")
      case Failure(cause) =>
        if (server.isCompleted) {
          val res = scala.util.Try(Await.result(server, scala.concurrent.duration.Duration.Inf)).failed.get
          fail(s"server failed: ${toCauses(res).map(_.getMessage()).mkString("\n  - ", "\n  - ", "\n")}")
        } else fail(s"$cause")
    }
  }

  override def beforeContainersStop(containers: Containers): Unit = {
    mockingbird.foreach(s => Await.ready(s.cancel(), scala.concurrent.duration.Duration.Inf))
    super.beforeContainersStop(containers)
  }

  generateTests(set)
}

object HttpStubSuite {
  def waitForReadiness(
      host: Uri,
      server: CancelableFuture[Nothing]
  ): RIO[SttpBackend[Task, ZioStreams with WebSockets], Unit] = {
    @annotation.nowarn("msg=a type was inferred to be `Any`")
    val policy = Schedule.fixed(3.second) && Schedule.recurs(10) && Schedule.recurWhile((_: Any) => !server.isCompleted)

    val probe = for {
      sb <- ZIO.service[SttpBackend[Task, ZioStreams with WebSockets]]
      _  <- zio.Console.printLine("waiting for server ready...")
      r  <- quickRequest.readTimeout(5.seconds.asScala).get(host.withPath("ready")).send(sb)
      _ <-
        if (r.code.code / 100 != 2) ZIO.fail(new Exception(s"waiting for 2xx, but got $r"))
        else ZIO.succeed(())
    } yield ()

    probe.retry(policy)
  }

  def initTestPrerequisites(
      host: Uri
  ): RIO[SttpBackend[Task, ZioStreams with WebSockets], Response[Either[String, String]]] =
    for {
      sb <- ZIO.service[SttpBackend[Task, ZioStreams with WebSockets]]
      r <- basicRequest
        .body("""{ "suffix": "alpha", "name": "Test Service" }""")
        .post(host.withPath("api/internal/mockingbird/v2/service".split("/")))
        .send(sb)
    } yield r

  def toCauses(src: Throwable): List[Throwable] = {
    val b = List.newBuilder[Throwable]
    var e = src
    while (e != null) {
      b += e
      e = e.getCause()
    }
    b.result()
  }
}
