package ru.tinkoff.tcb.mockingbird.dal2

import java.time.Instant
import scala.annotation.implicitNotFound
import scala.util.matching.Regex

import cats.tagless.autoFunctorK
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.numeric.Positive
import simulacrum.typeclass

import ru.tinkoff.tcb.dataaccess.UpdateResult
import ru.tinkoff.tcb.mockingbird.api.request.StubPatch
import ru.tinkoff.tcb.mockingbird.model.HttpMethod
import ru.tinkoff.tcb.mockingbird.model.HttpStub
import ru.tinkoff.tcb.mockingbird.model.Scope
import ru.tinkoff.tcb.utils.id.SID

/**
 * Потенциально заглушка может сопоставляться с мокируемым путям или как точное соответствие или регулярное выражения,
 * которому удовлетворяет путь на который пришел запрос.
 */
sealed trait StubPath extends Serializable with Product
final case class StubExactlyPath(value: String Refined NonEmpty) extends StubPath
final case class StubPathPattern(value: Regex) extends StubPath

/**
 * Параметры для поиска заглушек.
 *
 * @param scope
 * @param pathPattern
 *   представляет собой или строку, которая соответствует точному пути, или регулярное выражение. Заглушка может
 *   содержать или одно, или другое.
 * @param method
 */
final case class StubFindParams(scope: Scope, path: StubPath, method: HttpMethod)

/**
 * Параметры для подбора заглушек подходящих под указанный путь.
 *
 * @param scope
 * @param path
 *   Путь который был передан в mockingbird при вызове заглушки. Подходящая у подходящей заглушки поле path будет в
 *   точности равно этому пути или переданный путь будет соотвествовать регулярному выражению хранимому в поле
 *   pathPattern.
 * @param method
 */
final case class StubMatchParams(scope: Scope, path: String, method: HttpMethod)

/**
 * Параметры для отбора заглушек для отображения их списка в UI.
 *
 * @param page
 *   номер страницы для которой формируется список заглушек
 * @param query
 *   строка запроса, рассматривается как точный ID заглушки или используется как регулярное выражения и сопоставляется с
 *   полями name, path, pathPattern
 * @param service
 *   имя сервиса к которому относится заглушка (поле serviceSuffix)
 * @param labels
 *   список лейблов, которыми должна быть отмечена заглушка, все перечисленные лейблы должны содержаться в поле labels
 *   заглушки, хранящейся в хранилище
 * @param count
 *   количество заглушек, отображаемых на странице
 */
final case class StubFetchParams(
    page: Int Refined NonNegative,
    query: Option[String],
    service: Option[String],
    labels: Seq[String],
    count: Int Refined Positive
)

@implicitNotFound("Could not find an instance of HttpStubDAO for ${F}")
@typeclass @autoFunctorK
trait HttpStubDAO[F[_]] extends Serializable {

  /**
   * Возвращает заглушку по ID
   */
  def get(id: SID[HttpStub]): F[Option[HttpStub]]

  /**
   * Сохраняет заглушку в хранилище
   */
  def insert(stub: HttpStub): F[Long]

  /**
   * Обновляет заглушку, `patch` не содержит полей `created` и `serviceSuffix`.
   */
  def update(patch: StubPatch): F[UpdateResult]

  /**
   * Удаляет заглушку из хранилища
   */
  def delete(id: SID[HttpStub]): F[Long]

  /**
   * Удаляет Ephemeral и Countdown заглушки у которых `created` < `threshold`.
   *
   * @param threshold
   *   Максимальная дата создания заглушки которая должна остаться
   * @return
   *   количество удаленных записей
   */
  def deleteExpired(threshold: Instant): F[Long]

  /**
   * Удаляет Countdown заглушки у которых `times` <= 0
   *
   * @return
   *   количество удаленных записей
   */
  def deleteDepleted(): F[Long]

  /**
   * Возвращает список сохраненных заглушек, соответствующих преданным параметрам. Возвращает только заглушки для
   * который `times` > 0.
   *
   * P.S. При модификации заглушки запрос содержит условние на неравенство ID, но можно это потом проверить и на стороне
   * приложения, отфильтровать в полученных заглуках заглушку с определенным ID.
   */
  def find(params: StubFindParams): F[Vector[HttpStub]]

  /**
   * Возвращает заглушки для переданного `scope`, которые могут быть сопоставлены с указанным `path` и `method`. Под
   * сопоставлением понимается или совпадение с хранимым значением `path`, или, если переданный `path` удовлетворяет
   * регулярному выражению, хранимом в `pathPattern`. Так же у заглушки должен быть `times` > 0.
   */
  def findMatch(params: StubMatchParams): F[Vector[HttpStub]]

  /**
   * Возвращает заглушки для отображения в UI. Возвращает указанное в `params.count` количество, пропуская первые
   * `params.page * params.count` заглушек. Результат отсортирован по времени создания в обратном порядке - первыми идут
   * те заглушки, которые были созданы последними.
   */
  def fetch(params: StubFetchParams): F[Vector[HttpStub]]

  /**
   * Изменяет значения поля times у указанной заглушки на величину value
   *
   * @param id
   *   идентификатор заглушки
   * @param value
   *   значение на которое нужно изменить значение поля times
   * @return
   */
  def incTimesById(id: SID[HttpStub], value: Int): F[UpdateResult]
}

object HttpStubDAO {

  /* ======================================================================== */
  /* THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      */
  /* ======================================================================== */

  /**
   * Summon an instance of [[HttpStubDAO]] for `F`.
   */
  @inline def apply[F[_]](implicit instance: HttpStubDAO[F]): HttpStubDAO[F] = instance

  object ops {
    implicit def toAllHttpStubDAOOps[F[_], A](target: F[A])(implicit tc: HttpStubDAO[F]): AllOps[F, A] {
      type TypeClassType = HttpStubDAO[F]
    } = new AllOps[F, A] {
      type TypeClassType = HttpStubDAO[F]
      val self: F[A]                       = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  trait Ops[F[_], A] extends Serializable {
    type TypeClassType <: HttpStubDAO[F]
    def self: F[A]
    val typeClassInstance: TypeClassType
  }
  trait AllOps[F[_], A] extends Ops[F, A]
  trait ToHttpStubDAOOps extends Serializable {
    implicit def toHttpStubDAOOps[F[_], A](target: F[A])(implicit tc: HttpStubDAO[F]): Ops[F, A] {
      type TypeClassType = HttpStubDAO[F]
    } = new Ops[F, A] {
      type TypeClassType = HttpStubDAO[F]
      val self: F[A]                       = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToHttpStubDAOOps

  /* ======================================================================== */
  /* END OF SIMULACRUM-MANAGED CODE                                           */
  /* ======================================================================== */

}
