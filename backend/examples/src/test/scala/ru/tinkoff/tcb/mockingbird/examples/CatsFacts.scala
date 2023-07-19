package ru.tinkoff.tcb.mockingbird.examples

import ru.tinkoff.tcb.mockingbird.edsl.ExampleSet
import ru.tinkoff.tcb.mockingbird.edsl.model.*
import ru.tinkoff.tcb.mockingbird.edsl.model.Check.*
import ru.tinkoff.tcb.mockingbird.edsl.model.HttpMethod.*
import ru.tinkoff.tcb.mockingbird.edsl.model.ValueMatcher.syntax.*

class CatsFacts[HttpResponseR] extends ExampleSet[HttpResponseR] {

  override val name = "Примеры использования ExampleSet"

  example("Получение случайного факта о котиках")(
    for {
      _ <- describe("Отправить GET запрос")
      resp <- sendHttp(
        method = Get,
        path = "/fact",
        headers = Seq("X-CSRF-TOKEN" -> "unEENxJqSLS02rji2GjcKzNLc0C0ySlWih9hSxwn")
      )
      _ <- describe("Ответ содержит случайный факт полученный с сервера")
      _ <- checkHttp(
        resp,
        HttpResponseExpected(
          code = Some(CheckInteger(200)),
          body = Some(
            CheckJsonObject(
              "fact"   -> CheckJsonString("There are approximately 100 breeds of cat.".sample),
              "length" -> CheckJsonNumber(42.sample)
            )
          ),
          headers = Seq("Content-Type" -> CheckString("application/json"))
        )
      )
    } yield ()
  )
}
