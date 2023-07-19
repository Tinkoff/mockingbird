package ru.tinkoff.tcb.mockingbird.examples

import cats.syntax.all.*
import io.circe.parser

import ru.tinkoff.tcb.mockingbird.edsl.ExampleSet
import ru.tinkoff.tcb.mockingbird.edsl.model.*
import ru.tinkoff.tcb.mockingbird.edsl.model.Check.*
import ru.tinkoff.tcb.utils.circe.optics.*

class BasicHttpStub[HttpResponseR] extends ExampleSet[HttpResponseR] {
  import ValueMatcher.syntax.*

  val name = "Базовые примеры работы с HTTP заглушками"

  example("Persistent, ephemeral и countdown HTTP заглушки") {
    for {
      // TODO: подумать можно ли описать какие-то пререквизиты, чтобы в тестах
      // автоматически их проверять и возможно исполнять. Например, проверить
      // и создать сервис, если его нет, запустить сервис до которого будет
      // mockingbird проксировать запрос и т.п.
      _ <- describe("Предполагается, что в mockingbird есть сервис `alpha`.")

      _ <- describe("Создаем заглушку в скоупе `persistent`.")
      resp <- sendHttp(
        method = HttpMethod.Post,
        path = "/api/internal/mockingbird/v2/stub",
        body = """{
                 |  "path": "/alpha/handler1",
                 |  "name": "Persistent HTTP Stub",
                 |  "method": "GET",
                 |  "scope": "persistent",
                 |  "request": {
                 |    "mode": "no_body",
                 |    "headers": {}
                 |  },
                 |  "response": {
                 |    "mode": "raw",
                 |    "body": "persistent scope",
                 |    "headers": {
                 |      "Content-Type": "text/plain"
                 |    },
                 |    "code": "451"
                 |  }
                 |}""".stripMargin.some,
        headers = Seq(
          "Content-Type" -> "application/json",
        )
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckJsonObject(
            "status" -> CheckJsonString("success"),
            "id"     -> CheckJsonString("29dfd29e-d684-462e-8676-94dbdd747e30".sample)
          ).some,
        )
      )

      _ <- describe("Проверяем созданную заглушку.")
      resp <- sendHttp(
        method = HttpMethod.Get,
        path = "/api/mockingbird/exec/alpha/handler1",
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(451).some,
          body = CheckString("persistent scope").some,
          headers = Seq(
            "Content-Type" -> CheckString("text/plain"),
          ),
        )
      )

      _ <- describe("Для этого же пути, создаем заглушку в скоупе `ephemeral`.")
      resp <- sendHttp(
        method = HttpMethod.Post,
        path = "/api/internal/mockingbird/v2/stub",
        body = """{
                 |  "path": "/alpha/handler1",
                 |  "name": "Ephemeral HTTP Stub",
                 |  "method": "GET",
                 |  "scope": "ephemeral",
                 |  "request": {
                 |    "mode": "no_body",
                 |    "headers": {}
                 |  },
                 |  "response": {
                 |    "mode": "raw",
                 |    "body": "ephemeral scope",
                 |    "headers": {
                 |      "Content-Type": "text/plain"
                 |    },
                 |    "code": "200"
                 |  }
                 |}""".stripMargin.some,
        headers = Seq(
          "Content-Type" -> "application/json",
        )
      )
      r <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckJsonObject(
            "status" -> CheckJsonString("success"),
            "id"     -> CheckJsonString("13da7ef2-650e-4a54-9dca-377a1b1ca8b9".sample)
          ).some,
        )
      )
      idEphemeral = parser.parse(r.body.get).toOption.flatMap((JLens \ "id").getOpt).flatMap(_.asString).get

      _ <- describe("И создаем заглушку в скоупе `countdown` с `times` равным 2.")
      resp <- sendHttp(
        method = HttpMethod.Post,
        path = "/api/internal/mockingbird/v2/stub",
        body = """{
                 |  "path": "/alpha/handler1",
                 |  "times": 2,
                 |  "name": "Countdown Stub",
                 |  "method": "GET",
                 |  "scope": "countdown",
                 |  "request": {
                 |    "mode": "no_body",
                 |    "headers": {}
                 |  },
                 |  "response": {
                 |    "mode": "raw",
                 |    "body": "countdown scope",
                 |    "headers": {
                 |      "Content-Type": "text/plain"
                 |    },
                 |    "code": "429"
                 |  }
                 |}""".stripMargin.some,
        headers = Seq(
          "Content-Type" -> "application/json",
        )
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckJsonObject(
            "status" -> CheckJsonString("success"),
            "id"     -> CheckJsonString("09ec1cb9-4ca0-4142-b796-b94a24d9df29".sample)
          ).some,
        )
      )

      _ <- describe(
        """Заданные заглушки отличаются возвращаемыми ответами, а именно содержимым `body` и `code`,
        | в целом они могут быть как и полностью одинаковыми так и иметь больше различий.
        | Скоупы заглушек в порядке убывания приоритета: Countdown, Ephemeral, Persistent""".stripMargin
      )

      _ <- describe(
        """Так как заглушка `countdown` была создана с `times` равным двум, то следующие два
          |запроса вернут указанное в ней содержимое.""".stripMargin
      )
      // TODO: при генерации Markdown будет дважды добавлен запрос и ожидаемый ответ,
      // может стоит добавить действие Repeat, чтобы при генерации Markdown в документе
      // указывалось бы, что данное действие повторяется N раз.
      _ <- Seq
        .fill(2)(
          for {
            resp <- sendHttp(
              method = HttpMethod.Get,
              path = "/api/mockingbird/exec/alpha/handler1",
            )
            _ <- checkHttp(
              resp,
              HttpResponseExpected(
                code = CheckInteger(429).some,
                body = CheckString("countdown scope").some,
                headers = Seq(
                  "Content-Type" -> CheckString("text/plain"),
                ),
              )
            )
          } yield ()
        )
        .sequence

      _ <- describe(
        """Последующие запросы будут возвращать содержимое заглушки `ephemeral`. Если бы её не было,
          |то вернулся бы ответ от заглушки `persistent`.""".stripMargin
      )
      resp <- sendHttp(
        method = HttpMethod.Get,
        path = "/api/mockingbird/exec/alpha/handler1",
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckString("ephemeral scope").some,
          headers = Seq(
            "Content-Type" -> CheckString("text/plain"),
          ),
        )
      )

      _ <- describe("""Чтобы получить теперь ответ от `persistent` заглушки нужно или дождаться, когда истекут
          |сутки с момента её создания или просто удалить `ephemeral` заглушку.""".stripMargin)
      resp <- sendHttp(
        method = HttpMethod.Delete,
        path = s"/api/internal/mockingbird/v2/stub/$idEphemeral",
        headers = Seq(
          "Content-Type" -> "application/json",
        )
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckJsonObject(
            "status" -> CheckJsonString("success"),
            "id"     -> CheckJsonNull,
          ).some,
        )
      )

      _ <- describe("После удаления `ephemeral` заглушки, при запросе вернется результат заглушки `persistent`")
      resp <- sendHttp(
        method = HttpMethod.Get,
        path = "/api/mockingbird/exec/alpha/handler1",
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(451).some,
          body = CheckString("persistent scope").some,
          headers = Seq(
            "Content-Type" -> CheckString("text/plain"),
          ),
        )
      )
    } yield ()
  }

  example("Использование параметров пути в HTTP заглушках") {
    for {
      _ <- describe(
        """Заглушка может выбираться в том числе и на основании регулярного выражения
          |в пути, это может быть не очень эффективно с точки зрения поиска такой заглушки.
          |Поэтому без необходимости, лучше не использовать этот механизм.""".stripMargin
      )

      _ <- describe("Предполагается, что в mockingbird есть сервис `alpha`.")

      _ <- describe(
        """Скоуп в котором создаются заглушки не важен. В целом скоуп влияет только
          |на приоритет заглушек. В данном случае заглушка создается в скоупе `countdown`.
          |В отличие от предыдущих примеров, здесь для указания пути для срабатывания
          |заглушки используется поле `pathPattern`, вместо `path`. Так же, ответ который
          |формирует заглушка не статичный, а зависит от параметров пути.""".stripMargin
      )

      resp <- sendHttp(
        method = HttpMethod.Post,
        path = "/api/internal/mockingbird/v2/stub",
        body = """{
                 |  "pathPattern": "/alpha/handler2/(?<obj>[-_A-z0-9]+)/(?<id>[0-9]+)",
                 |  "times": 2,
                 |  "name": "Simple HTTP Stub with path pattern",
                 |  "method": "GET",
                 |  "scope": "countdown",
                 |  "request": {
                 |    "mode": "no_body",
                 |    "headers": {}
                 |  },
                 |  "response": {
                 |    "mode": "json",
                 |    "body": {
                 |      "static_field": "Fixed part of reponse",
                 |      "obj": "${pathParts.obj}",
                 |      "id": "${pathParts.id}"
                 |    },
                 |    "headers": {
                 |      "Content-Type": "application/json"
                 |    },
                 |    "code": "200"
                 |  }
                 |}""".stripMargin.some,
        headers = Seq(
          "Content-Type" -> "application/json",
        )
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckJsonObject(
            "status" -> CheckJsonString("success"),
            "id"     -> CheckJsonString("c8c9d92f-192e-4fe3-8a09-4c9b69802603".sample)
          ).some,
        )
      )

      _ <- describe(
        """Теперь сделаем несколько запросов, который приведут к срабатыванию этой заглшки,
          |чтобы увидеть, что результат действительно зависит от пути.""".stripMargin
      )
      resp <- sendHttp(
        method = HttpMethod.Get,
        path = "/api/mockingbird/exec/alpha/handler2/alpha/123",
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckJsonObject(
            "static_field" -> CheckJsonString("Fixed part of reponse"),
            "obj"          -> CheckJsonString("alpha"),
            "id"           -> CheckJsonString("123")
          ).some,
          headers = Seq("Content-Type" -> CheckString("application/json")),
        )
      )
      resp <- sendHttp(
        method = HttpMethod.Get,
        path = "/api/mockingbird/exec/alpha/handler2/beta/876",
      )
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = CheckInteger(200).some,
          body = CheckJsonObject(
            "static_field" -> CheckJsonString("Fixed part of reponse"),
            "obj"          -> CheckJsonString("beta"),
            "id"           -> CheckJsonString("876")
          ).some,
          headers = Seq("Content-Type" -> CheckString("application/json")),
        )
      )
    } yield ()
  }
}
