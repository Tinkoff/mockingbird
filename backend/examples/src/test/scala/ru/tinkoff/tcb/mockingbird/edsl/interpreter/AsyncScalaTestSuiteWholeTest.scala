package ru.tinkoff.tcb.mockingbird.edsl.interpreter

import scala.concurrent.Future

import org.scalactic.source
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FutureOutcome
import org.scalatest.Informer
import org.scalatest.matchers.should.Matchers
import sttp.capabilities.WebSockets
import sttp.client3.*
import sttp.client3.testing.SttpBackendStub
import sttp.model.Header
import sttp.model.MediaType
import sttp.model.Method.*
import sttp.model.StatusCode
import sttp.model.Uri

import ru.tinkoff.tcb.mockingbird.examples.CatsFacts

class AsyncScalaTestSuiteWholeTest
    extends AsyncScalaTestSuite
    with Matchers
    with AsyncMockFactory
    with BeforeAndAfterAll {

  val eset = new CatsFacts[HttpResponseR]()

  var sttpbackend_ : SttpBackendStub[Future, WebSockets] =
    HttpClientFutureBackend.stub()

  override private[interpreter] def sttpbackend: SttpBackend[Future, WebSockets] = sttpbackend_

  override def baseUri: Uri = uri"https://localhost.example:9977"

  var mockInformer: Option[Informer]    = none
  override protected def info: Informer = mockInformer.getOrElse(super.info)

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    val mockI = mock[Informer]

    (mockI
      .apply(_: String, _: Option[Any])(_: source.Position))
      .expects("Отправить GET запрос", *, *)
      .once()

    (mockI
      .apply(_: String, _: Option[Any])(_: source.Position))
      .expects("Ответ содержит случайный факт полученный с сервера", *, *)
      .twice()

    mockInformer = mockI.some

    sttpbackend_ = HttpClientFutureBackend
      .stub()
      .whenRequestMatches { req =>
        req.method == GET && req.uri.toString() == s"https://localhost.example:9977/fact" &&
        req.headers.exists(h => h.name == "X-CSRF-TOKEN" && h.value == "unEENxJqSLS02rji2GjcKzNLc0C0ySlWih9hSxwn")
      }
      .thenRespond(
        Response(
          body = """{
                   |  "fact" : "There are approximately 100 breeds of cat.",
                   |  "length" : 42.0
                   |}""".stripMargin,
          code = StatusCode.Ok,
          statusText = "",
          headers = Seq(Header.contentType(MediaType.ApplicationJson))
        )
      )
  }

  override protected def afterAll(): Unit = {
    calledTests shouldBe Vector("fake", "Получение случайного факта о котиках")

    super.afterAll()
  }

  private var calledTests: Vector[String] = Vector.empty

  override def withFixture(test: NoArgAsyncTest): FutureOutcome = {
    calledTests = calledTests :+ test.name
    test()
  }

  test("fake") {
    // The afterAll isn't called if the test suite doesn't contain any test.
    // It happens If the generateTest doesn't add any test. It's reason
    // why this test added. Its existence prevents this case.
    Future(succeed)
  }

  generateTests(eset)

}
