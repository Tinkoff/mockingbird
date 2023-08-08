package ru.tinkoff.tcb.mockingbird.dal2

import scala.annotation.implicitNotFound

import cats.tagless.autoFunctorK
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.Json
import simulacrum.typeclass

import ru.tinkoff.tcb.dataaccess.UpdateResult
import ru.tinkoff.tcb.mockingbird.model.PersistentState
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.utils.id.SID

object PersistentStateDAO {
  type Predicate = Map[JsonOptic, Map[Keyword.Json, Json]]

  /* ======================================================================== */
  /* THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      */
  /* ======================================================================== */

  /**
   * Summon an instance of [[PersistentStateDAO]] for `F`.
   */
  @inline def apply[F[_]](implicit instance: PersistentStateDAO[F]): PersistentStateDAO[F] = instance

  object ops {
    implicit def toAllPersistentStateDAOOps[F[_], A](target: F[A])(implicit tc: PersistentStateDAO[F]): AllOps[F, A] {
      type TypeClassType = PersistentStateDAO[F]
    } = new AllOps[F, A] {
      type TypeClassType = PersistentStateDAO[F]
      val self: F[A]                       = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  trait Ops[F[_], A] extends Serializable {
    type TypeClassType <: PersistentStateDAO[F]
    def self: F[A]
    val typeClassInstance: TypeClassType
  }
  trait AllOps[F[_], A] extends Ops[F, A]
  trait ToPersistentStateDAOOps extends Serializable {
    implicit def toPersistentStateDAOOps[F[_], A](target: F[A])(implicit tc: PersistentStateDAO[F]): Ops[F, A] {
      type TypeClassType = PersistentStateDAO[F]
    } = new Ops[F, A] {
      type TypeClassType = PersistentStateDAO[F]
      val self: F[A]                       = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToPersistentStateDAOOps

  /* ======================================================================== */
  /* END OF SIMULACRUM-MANAGED CODE                                           */
  /* ======================================================================== */

}

@implicitNotFound("Could not find an instance of PersistentStateDAO for ${F}")
@typeclass @autoFunctorK
trait PersistentStateDAO[F[_]] extends Serializable {
  import PersistentStateDAO.Predicate

  /**
   * Поиск состояния по предикату. (В dal.PersistentStateDAO называется findBySpec)
   *
   * @param p
   *   Предикат
   * @return
   *   Список найденных состояний удовлетворяющих заданному предикату.
   */
  def find(p: Predicate): F[Vector[PersistentState]]

  /**
   * Создать или перезаписать хранимое состояние в БД. (В dal.PersistentStateDAO называется upsertBySpec)
   *
   * @param id
   *   Идентификатор хранимого состояния
   * @param s
   *   Само состояние
   * @return
   */
  def upsert(id: SID[PersistentState], s: Json): F[UpdateResult]

  /**
   * Создать индекс для поиска хранимых состояний по указанному полю.
   *
   * @param field
   *   Поле по которому будет строится индекс. В общем случае, это может быть путь в JSON, а не просто поле первого
   *   уровня.
   * @return
   */
  def createIndexForDataField(field: NonEmptyString): F[Unit]
}
