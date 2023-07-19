package ru.tinkoff.tcb.mockingbird.edsl.model
import io.circe.Json

sealed trait ValueMatcher[T] extends Product with Serializable
object ValueMatcher {

  /**
   * Показывает, что ожидается конкретное значение типа `T`, в случае несовпадения сгенерированный тест упадет с
   * ошибкой.
   *
   * @param value
   *   значение используемое для сравнения и отображения при генерации примера ответа от сервера в markdown.
   */
  final case class FixedValue[T](value: T) extends ValueMatcher[T]

  /**
   * Показывает, что ожидается любое значение типа `T`.
   *
   * @param example
   *   Это значение будет отображено в markdown документе при генерации в описании примера ответа от сервера.
   */
  final case class AnyValue[T](example: T) extends ValueMatcher[T]

  object syntax {
    implicit class ValueMatcherBuilder[T](private val v: T) extends AnyVal {
      def fixed: ValueMatcher[T]  = FixedValue(v)
      def sample: ValueMatcher[T] = AnyValue(v)
    }

    implicit def buildFixed[T](v: T): ValueMatcher[T] = ValueMatcher.FixedValue(v)

    implicit def convertion[A, B](vm: ValueMatcher[A])(implicit f: A => B): ValueMatcher[B] =
      vm match {
        case FixedValue(a) => FixedValue(f(a))
        case AnyValue(a)   => AnyValue(f(a))
      }
  }
}

sealed trait Check extends Product with Serializable
object Check {

  /**
   * Соответствует любому значению.
   *
   * @param example
   *   значение, которое будет использоваться как пример при генерации Markdown.
   * @group CheckCommon
   */
  final case class CheckAny(example: String) extends Check

  /**
   * @group CheckCommon
   */
  final case class CheckString(matcher: ValueMatcher[String]) extends Check

  /**
   * @group CheckCommon
   */
  final case class CheckInteger(matcher: ValueMatcher[Long]) extends Check

  /**
   * Показывает, что ожидается JSON, реализации этого трейта позволяют детальнее описать ожидания.
   * @group CheckJson
   */
  sealed trait CheckJson extends Check

  /**
   * Значение null
   * @group CheckJson
   */
  final case object CheckJsonNull extends CheckJson

  /**
   * Любой валидный JSON.
   *
   * @constructor
   * @param example
   *   значение, которое будет использоваться как пример при генерации Markdown.
   * @group CheckJson
   */
  final case class CheckJsonAny(example: Json) extends CheckJson

  /**
   * JSON объект с указанными полями, объект с которым производится сравнение может содержать дополнительные поля.
   * @group CheckJson
   */
  final case class CheckJsonObject(fields: (String, CheckJson)*) extends CheckJson

  /**
   * Массив с указанными элементами, важен порядок. Проверяемы массив может содержать в конце дополнительные элементы.
   * @group CheckJson
   */
  final case class CheckJsonArray(items: CheckJson*) extends CheckJson

  /**
   * @group CheckJson
   */
  final case class CheckJsonString(matcher: ValueMatcher[String]) extends CheckJson

  /**
   * @group CheckJson
   */
  final case class CheckJsonNumber(matcher: ValueMatcher[Double]) extends CheckJson
}
