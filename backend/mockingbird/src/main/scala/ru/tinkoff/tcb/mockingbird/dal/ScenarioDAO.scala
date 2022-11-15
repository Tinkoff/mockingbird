package ru.tinkoff.tcb.mockingbird.dal

import scala.annotation.implicitNotFound

import cats.tagless.autoFunctorK
import com.github.dwickern.macros.NameOf.*
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Indexes.*
import simulacrum.typeclass

import ru.tinkoff.tcb.mockingbird.model.Scenario
import ru.tinkoff.tcb.mongo.DAOBase
import ru.tinkoff.tcb.mongo.MongoDAO

@implicitNotFound("Could not find an instance of ScenarioDAO for ${F}")
@typeclass @autoFunctorK
trait ScenarioDAO[F[_]] extends MongoDAO[F, Scenario]

object ScenarioDAO {
  /* ======================================================================== */
  /* THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      */
  /* ======================================================================== */

  /**
   * Summon an instance of [[ScenarioDAO]] for `F`.
   */
  @inline def apply[F[_]](implicit instance: ScenarioDAO[F]): ScenarioDAO[F] = instance

  object ops {
    implicit def toAllScenarioDAOOps[F[_], A](target: F[A])(implicit tc: ScenarioDAO[F]): AllOps[F, A] {
      type TypeClassType = ScenarioDAO[F]
    } = new AllOps[F, A] {
      type TypeClassType = ScenarioDAO[F]
      val self: F[A]                       = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  trait Ops[F[_], A] extends Serializable {
    type TypeClassType <: ScenarioDAO[F]
    def self: F[A]
    val typeClassInstance: TypeClassType
  }
  trait AllOps[F[_], A] extends Ops[F, A]
  trait ToScenarioDAOOps extends Serializable {
    implicit def toScenarioDAOOps[F[_], A](target: F[A])(implicit tc: ScenarioDAO[F]): Ops[F, A] {
      type TypeClassType = ScenarioDAO[F]
    } = new Ops[F, A] {
      type TypeClassType = ScenarioDAO[F]
      val self: F[A]                       = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToScenarioDAOOps

  /* ======================================================================== */
  /* END OF SIMULACRUM-MANAGED CODE                                           */
  /* ======================================================================== */

}

class ScenarioDAOImpl(collection: MongoCollection[BsonDocument])
    extends DAOBase[Scenario](collection)
    with ScenarioDAO[Task] {
  def createIndexes: Task[Unit] =
    createIndex(
      ascending(nameOf[Scenario](_.source), nameOf[Scenario](_.scope))
    ) *> createIndex(
      descending(nameOf[Scenario](_.created))
    ) *> createIndex(
      ascending(nameOf[Scenario](_.service))
    ) *> createIndex(
      ascending(nameOf[Scenario](_.labels))
    )
}

object ScenarioDAOImpl {
  val live = ZLayer {
    for {
      mc <- ZIO.service[MongoCollection[BsonDocument]]
      sd = new ScenarioDAOImpl(mc)
      _ <- sd.createIndexes
    } yield sd.asInstanceOf[ScenarioDAO[Task]]
  }
}
