package ru.tinkoff.tcb.mockingbird.edsl.interpreter

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import pl.muninn.scalamdtag.*
import sttp.client3.*

import ru.tinkoff.tcb.mockingbird.edsl.ExampleSet
import ru.tinkoff.tcb.mockingbird.edsl.model.*
import ru.tinkoff.tcb.mockingbird.edsl.model.Check.*
import ru.tinkoff.tcb.mockingbird.edsl.model.ValueMatcher.syntax.*
import ru.tinkoff.tcb.mockingbird.examples.CatsFacts

class MarkdownGeneratorSuite extends AnyFunSuite with Matchers {
  val eset = new ExampleSet[MarkdownGenerator.HttpResponseR] {
    override def name: String = ""
  }

  test("describe produces text as markdown paragraph") {

    val mdg = MarkdownGenerator(uri"http://localhost")
    val text = """
                 |
                 |
                 |Sea at omnes semper causae. Eleifend `inimicus` ea mea, ut zril nemore qui. Ne
                 |odio enim has. Probo ignota phaedrum no pri, ei eam tale luptatum moderatius.
                 |Et elit postea sensibus sea, et his malis luptatum.
                 |
                 |
                 |Vis prima vituperata ad. No sed debitis `gloriatur` intellegat. Et per volumus
                 |dissentiet, ei audiam diceret vim. Sed wisi falli ex. Vis noster eirmod ex, eos
                 |euismod ponderum eu.
                 |
                 |
                 |""".stripMargin

    val mds = eset.describe(text).foldMap(mdg.stepsPrinterW).written
    mds should have length 1
    mds.head.md shouldBe ("\n" ++ text ++ "\n")
  }

  test("sendHttp produces curl command") {
    val host   = "http://localhost:8080"
    val mdg    = MarkdownGenerator(uri"$host")
    val method = HttpMethod.Post
    val path   = "/api/handler"
    val body = """{
                 |  "foo": [],
                 |  "bar": 42
                 |}""".stripMargin
    val headers = Seq("x-token" -> "asd5453qwe", "Content-Type" -> "application/json")
    val query   = Seq("page" -> "3", "limit" -> "10", "service" -> "world")

    val example = eset.sendHttp(method, path, body.some, headers, query)

    val mds = example
      .foldMap(mdg.stepsPrinterW)
      .written
    mds should have length 1

    val obtains = mds.head.md
    val expected =
      raw"""```
           |curl \
           |  --request POST \
           |  --url '$host$path?${query.map { case (n, v) => s"$n=$v" }.mkString("&")}' \
           |  --header 'x-token: asd5453qwe' \
           |  --header 'Content-Type: application/json' \
           |  --data-raw '$body'
           |
           |```""".stripMargin
    obtains shouldBe expected
  }

  test("checkHttp without expectation produces nothing") {
    val mdg = MarkdownGenerator(uri"http://localhost:8080")

    val example = eset.checkHttp(MarkdownGenerator.httpResponseR, HttpResponseExpected(None, None, Seq.empty))

    val mds = example
      .foldMap(mdg.stepsPrinterW)
      .written

    mds shouldBe empty
  }

  test("checkHttp with expectation produces code block") {
    val mdg = MarkdownGenerator(uri"http://localhost:8080")

    val example = eset.checkHttp(
      MarkdownGenerator.httpResponseR,
      HttpResponseExpected(
        code = CheckInteger(418).some,
        body = CheckJsonObject(
          "foo" -> CheckJsonArray(),
          "bar" -> CheckJsonNull,
          "inner" -> CheckJsonObject(
            "i1" -> CheckJsonString("some string"),
            "xx" -> CheckJsonNumber(23.0.sample),
          ),
        ).some,
        headers = Seq(
          "Content-Type" -> CheckString("application/json"),
          "token"        -> CheckString("token-example".sample),
        ),
      )
    )

    val mds = example
      .foldMap(mdg.stepsPrinterW)
      .written
    mds should have length 2

    val obtains = markdown(mds).md
    val expected =
      raw"""
           |Ответ:
           |```
           |Код ответа: 418
           |
           |Заголовки ответа:
           |Content-Type: 'application/json'
           |token: 'token-example'
           |
           |Тело ответа:
           |{
           |  "foo" : [
           |  ],
           |  "bar" : null,
           |  "inner" : {
           |    "i1" : "some string",
           |    "xx" : 23.0
           |  }
           |}
           |
           |```
           |""".stripMargin
    obtains shouldBe expected
  }

  test("whole HTTP example") {
    val mdg = MarkdownGenerator(uri"https://catfact.ninja")
    val set = new CatsFacts[MarkdownGenerator.HttpResponseR]()
    mdg.generate(set) shouldBe
      """# Примеры использования ExampleSet
        |## Получение случайного факта о котиках
        |
        |Отправить GET запрос
        |```
        |curl \
        |  --request GET \
        |  --url 'https://catfact.ninja/fact' \
        |  --header 'X-CSRF-TOKEN: unEENxJqSLS02rji2GjcKzNLc0C0ySlWih9hSxwn'
        |
        |```
        |
        |Ответ содержит случайный факт полученный с сервера
        |
        |Ответ:
        |```
        |Код ответа: 200
        |
        |Заголовки ответа:
        |Content-Type: 'application/json'
        |
        |Тело ответа:
        |{
        |  "fact" : "There are approximately 100 breeds of cat.",
        |  "length" : 42.0
        |}
        |
        |```
        |""".stripMargin
  }
}
