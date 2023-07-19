package ru.tinkoff.tcb.mockingbird.edsl

import cats.free.Free.liftF
import org.scalactic.source

import ru.tinkoff.tcb.mockingbird.edsl.model.*

/**
 * ==Описание набора примеров==
 *
 * `ExampleSet` предоставляет DSL для описания примеров взаимодействия с Mockingbird со стороны внешнего
 * приложения/пользователя через его API. Описанные примеры потом можно в Markdown описание последовательности действий
 * с примерами HTTP запросов и ответов на них или сгенерировать тесты для scalatest. За это отвечают интерпретаторы DSL
 * [[ru.tinkoff.tcb.mockingbird.edsl.interpreter.MarkdownGenerator MarkdownGenerator]] и
 * [[ru.tinkoff.tcb.mockingbird.edsl.interpreter.AsyncScalaTestSuite AsyncScalaTestSuite]] соответственно.
 *
 * Описание набора примеров может выглядеть так:
 *
 * {{{
 * package ru.tinkoff.tcb.mockingbird.examples
 *
 * import ru.tinkoff.tcb.mockingbird.edsl.ExampleSet
 * import ru.tinkoff.tcb.mockingbird.edsl.model.*
 * import ru.tinkoff.tcb.mockingbird.edsl.model.Check.*
 * import ru.tinkoff.tcb.mockingbird.edsl.model.HttpMethod.*
 * import ru.tinkoff.tcb.mockingbird.edsl.model.ValueMatcher.syntax.*
 *
 * class CatsFacts[HttpResponseR] extends ExampleSet[HttpResponseR] {
 *
 *   override val name = "Примеры использования ExampleSet"
 *
 *   example("Получение случайного факта о котиках")(
 *     for {
 *       _ <- describe("Отправить GET запрос")
 *       resp <- sendHttp(
 *         method = Get,
 *         path = "/fact",
 *         headers = Seq("X-CSRF-TOKEN" -> "unEENxJqSLS02rji2GjcKzNLc0C0ySlWih9hSxwn")
 *       )
 *       _ <- describe("Ответ содержит случайный факт полученный с сервера")
 *       _ <- checkHttp(
 *         resp,
 *         HttpResponseExpected(
 *           code = Some(CheckInteger(200)),
 *           body = Some(
 *             CheckJsonObject(
 *               "fact"   -> CheckJsonString("There are approximately 100 breeds of cat.".sample),
 *               "length" -> CheckJsonNumber(42.sample)
 *             )
 *           ),
 *           headers = Seq("Content-Type" -> CheckString("application/json"))
 *         )
 *       )
 *     } yield ()
 *   )
 * }
 * }}}
 *
 * Дженерик параметр `HttpResponseR` нужен так результат выполнения HTTP запроса зависит от интерпретатора DSL.
 *
 * Переменная `name` - общий заголовок для примеров внутри набора, при генерации Markdown файла будет добавлен в самое
 * начало как заголовок первого уровня.
 *
 * Метод `example` позволяет добавить пример к набору. Вначале указывается название примера, как первый набор
 * аргументов. При генерации тестов это будет именем теста, а при генерации Markdown будет добавлено как заголовок
 * второго уровня, затем описывается сам пример. Последовательность действий описывается при помощи монады
 * [[ru.tinkoff.tcb.mockingbird.edsl.Example Example]].
 *
 * `ExampleSet` предоставляет следующие действия:
 *   - [[describe]] - добавить текстовое описание.
 *   - [[sendHttp]] - исполнить HTTP запрос с указанными параметрами, возвращает результат запроса.
 *   - [[checkHttp]] - проверить, что результат запроса отвечает указанным ожиданиям, возвращает извлеченные из ответа
 *     данные на основании проверок. ''Если предполагается использовать какие-то части ответа по ходу описания примера,
 *     то необходимо для них задать ожидания, иначе они будут отсутствовать в возвращаемом объекте.''
 *
 * Для описания ожиданий используются проверки [[model.Check$]]. Некоторые проверки принимают как параметр
 * [[model.ValueMatcher ValueMatcher]]. Данный трейт тип представлен двумя реализациями
 * [[model.ValueMatcher.AnyValue AnyValue]] и [[model.ValueMatcher.FixedValue FixedValue]]. Первая описывает
 * произвольное значение определенного типа, т.е. проверки значения не производится. Вторая задает конкретное ожидаемое
 * значение.
 *
 * Для упрощения создания значений типа [[model.ValueMatcher ValueMatcher]] добавлены имплиситы в объекте
 * [[model.ValueMatcher.syntax ValueMatcher.syntax]]. Они добавляют неявную конвертацию значений в тип
 * [[model.ValueMatcher.FixedValue FixedValue]], а так же методы `sample` и `fixed` для создания
 * [[model.ValueMatcher.AnyValue AnyValue]] и [[model.ValueMatcher.FixedValue FixedValue]] соответственно. Благодаря
 * этому можно писать:
 * {{{
 *   CheckString("some sample".sample) // вместо CheckString(AnyValue("some sample"))
 *   CheckString("some fixed string") // вместо CheckString(FixedValue("some fixed string"))
 * }}}
 *
 * ==Генерации markdown документа из набора примеров==
 *
 * {{{
 * package ru.tinkoff.tcb.mockingbird.examples
 *
 * import sttp.client3.*
 *
 * import ru.tinkoff.tcb.mockingbird.edsl.interpreter.MarkdownGenerator
 *
 * object CatsFactsMd {
 *   def main(args: Array[String]): Unit = {
 *     val mdg = MarkdownGenerator(baseUri = uri"https://catfact.ninja")
 *     val set = new CatsFacts[MarkdownGenerator.HttpResponseR]()
 *     println(mdg.generate(set))
 *   }
 * }
 * }}}
 *
 * Здесь создается интерпретатор [[ru.tinkoff.tcb.mockingbird.edsl.interpreter.MarkdownGenerator MarkdownGenerator]] для
 * генерации markdown документа из инстанса `ExampleSet`. Как параметр, конструктору передается хост со схемой который
 * будет подставлен в качестве примера в документ.
 *
 * Как упоминалось ранее, тип ответа от HTTP сервера зависит от интерпретатора DSL, поэтому при создании `CatsFacts`
 * параметром передается тип `MarkdownGenerator.HttpResponseR`.
 *
 * ==Генерация тестов из набора примеров==
 * {{{
 * package ru.tinkoff.tcb.mockingbird.examples
 *
 * import sttp.client3.*
 *
 * import ru.tinkoff.tcb.mockingbird.edsl.interpreter.AsyncScalaTestSuite
 *
 * class CatsFactsSuite extends AsyncScalaTestSuite {
 *   override val baseUri = uri"https://catfact.ninja"
 *   val set              = new CatsFacts[HttpResponseR]()
 *   generateTests(set)
 * }
 * }}}
 *
 * Для генерации тестов нужно создать класс и унаследовать его от
 * [[ru.tinkoff.tcb.mockingbird.edsl.interpreter.AsyncScalaTestSuite AsyncScalaTestSuite]]. После чего в переопределить
 * значение `baseUri` и в конструкторе вызвать метод `generateTests` передав в него набор примеров. В качестве дженерик
 * параметра для типа HTTP ответа, в создаваемый инстанс набора примеров надо передать тип
 * [[ru.tinkoff.tcb.mockingbird.edsl.interpreter.AsyncScalaTestSuite.HttpResponseR AsyncScalaTestSuite.HttpResponseR]]
 *
 * Пример запуска тестов:
 * {{{
 * [info] CatsFactsSuite:
 * [info] - Получение случайного факта о котиках
 * [info]   + Отправить GET запрос
 * [info]   + Ответ содержит случайный факт полученный с сервера
 * [info] Run completed in 563 milliseconds.
 * [info] Total number of tests run: 1
 * [info] Suites: completed 1, aborted 0
 * [info] Tests: succeeded 1, failed 0, canceled 0, ignored 0, pending 0
 * [info] All tests passed.
 * }}}
 */
trait ExampleSet[HttpResponseR] {
  private var examples_ : Vector[ExampleDescription] = Vector.empty

  final private[edsl] def examples: Vector[ExampleDescription] = examples_

  /**
   * Заглавие набора примеров.
   */
  def name: String

  final protected def example(name: String)(body: Example[Any])(implicit pos: source.Position): Unit =
    examples_ = examples_ :+ ExampleDescription(name, body, pos)

  /**
   * Выводит сообщение при помощи `info` при генерации тестов или добавляет текстовый блок при генерации Markdown.
   * @param text
   *   текст сообщения
   */
  final def describe(text: String)(implicit pos: source.Position): Example[Unit] =
    liftF[Step, Unit](Describe(text, pos))

  /**
   * В тестах, выполняет HTTP запрос с указанными параметрами или добавляет в Markdown пример запроса, который можно
   * исполнить командой `curl`.
   *
   * @param method
   *   используемый HTTP метод.
   * @param path
   *   путь до ресурса без схемы и хоста.
   * @param body
   *   тело запроса как текст.
   * @param headers
   *   заголовки, который будут переданы вместе с запросом.
   * @param query
   *   URL параметры запроса
   * @return
   *   возвращает объект представляющий собой результат исполнения запроса, конкретный тип зависит от интерпретатора
   *   DSL. Использовать возвращаемое значение можно только передав в метод [[checkHttp]].
   */
  final def sendHttp(
      method: HttpMethod,
      path: String,
      body: Option[String] = None,
      headers: Seq[(String, String)] = Seq.empty,
      query: Seq[(String, String)] = Seq.empty,
  )(implicit
      pos: source.Position
  ): Example[HttpResponseR] =
    liftF[Step, HttpResponseR](SendHttp[HttpResponseR](HttpRequest(method, path, body, headers, query), pos))

  /**
   * В тестах, проверяет, что полученный HTTP ответ соответствует ожиданиям. При генерации Markdown вставляет ожидаемый
   * ответ опираясь на указанные ожидания. Если никакие ожидания не указана, то ничего добавлено не будет.
   *
   * @param response
   *   результат исполнения [[sendHttp]], тип зависит от интерпретатора DSL.
   * @param expects
   *   ожидания предъявляемые к результату HTTP запроса. Ожидания касаются кода ответа, тела запроса и заголовков
   *   полеченных от сервера.
   * @return
   *   возвращает разобранный ответ от сервера. При генерации Markdown, так как реального ответа от сервера нет, то
   *   формирует ответ на основании переданных ожиданий от ответа. В Markdown добавляется информация только от том, для
   *   чего была указана проверка.
   */
  final def checkHttp(response: HttpResponseR, expects: HttpResponseExpected)(implicit
      pos: source.Position
  ): Example[HttpResponse] =
    liftF[Step, HttpResponse](CheckHttp(response, expects, pos))

}
