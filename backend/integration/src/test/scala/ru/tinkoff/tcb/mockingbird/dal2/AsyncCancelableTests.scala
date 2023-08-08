package ru.tinkoff.tcb.mockingbird.dal2

import eu.timepit.refined.auto.*
import eu.timepit.refined.types.string.NonEmptyString
import org.scalatest.EitherValues.*
import org.scalatest.FutureOutcome
import org.scalatest.funsuite.AsyncFunSuiteLike

/**
 * Данный трейт добавляет возожность пропустить исполнение тестов по их имени. Актуально для случая когда тесты описаны
 * в трейте и в зависимости от имплементации некоторые тесты можно пропустить. Пример использования:
 * {{{
 * import eu.timepit.refined.auto.*
 * import eu.timepit.refined.types.string.NonEmptyString
 *
 * class ConcreateBehaviorsImplSuite
 *     extends AsyncFunSuite
 *     with BehavioursSuite
 *     with AsyncCancelableTests {
 *
 *   override val caceledTests: Map[TestName, CacnelTestReason] = Map(
 *     NonEmptyString("Test #3") -> "It is canceled, because the concrete implementation doesn't support this case."
 *   )
 *
 * }
 * }}}
 */
trait AsyncCancelableTests { self: AsyncFunSuiteLike =>
  type TestName         = NonEmptyString
  type CancelTestReason = NonEmptyString

  /**
   * canceledTests возвращает словарь содержащий имена тестов, которые необходимо отменить и причину отмены. Это может
   * быть связано с тем, что конкретная реализация не поддерживает требуемую функциональность или это работает иначе, но
   * без ущерба для конечного пользователя mockingbird.
   */
  def canceledTests: Map[TestName, CancelTestReason] = Map.empty

  override def withFixture(test: NoArgAsyncTest): FutureOutcome =
    canceledTests.get(NonEmptyString.from(test.name).value).map(FutureOutcome.canceled(_)).getOrElse(test())
}
