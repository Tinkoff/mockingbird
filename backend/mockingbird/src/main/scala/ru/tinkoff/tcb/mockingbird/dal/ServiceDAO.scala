package ru.tinkoff.tcb.mockingbird.dal

import scala.annotation.implicitNotFound
import scala.util.matching.Regex

import cats.tagless.autoFunctorK
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.BsonDocument
import simulacrum.typeclass

import ru.tinkoff.tcb.mockingbird.model.Service
import ru.tinkoff.tcb.mongo.DAOBase
import ru.tinkoff.tcb.mongo.MongoDAO

@implicitNotFound("Could not find an instance of ServiceDAO for ${F}")
@typeclass @autoFunctorK
trait ServiceDAO[F[_]] extends MongoDAO[F, Service] {
  def getServiceFor(path: String): F[Option[Service]]
  def getServiceFor(pattern: Regex): F[Option[Service]]
}

object ServiceDAO {
  /* ======================================================================== */
  /* THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      */
  /* ======================================================================== */

  /**
   * Summon an instance of [[ServiceDAO]] for `F`.
   */
  @inline def apply[F[_]](implicit instance: ServiceDAO[F]): ServiceDAO[F] = instance

  object ops {
    implicit def toAllServiceDAOOps[F[_], A](target: F[A])(implicit tc: ServiceDAO[F]): AllOps[F, A] {
      type TypeClassType = ServiceDAO[F]
    } = new AllOps[F, A] {
      type TypeClassType = ServiceDAO[F]
      val self: F[A]                       = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  trait Ops[F[_], A] extends Serializable {
    type TypeClassType <: ServiceDAO[F]
    def self: F[A]
    val typeClassInstance: TypeClassType
  }
  trait AllOps[F[_], A] extends Ops[F, A]
  trait ToServiceDAOOps extends Serializable {
    implicit def toServiceDAOOps[F[_], A](target: F[A])(implicit tc: ServiceDAO[F]): Ops[F, A] {
      type TypeClassType = ServiceDAO[F]
    } = new Ops[F, A] {
      type TypeClassType = ServiceDAO[F]
      val self: F[A]                       = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToServiceDAOOps

  /* ======================================================================== */
  /* END OF SIMULACRUM-MANAGED CODE                                           */
  /* ======================================================================== */

}

class ServiceDAOImpl(collection: MongoCollection[BsonDocument])
    extends DAOBase[Service](collection)
    with ServiceDAO[Task] {
  override def getServiceFor(path: String): Task[Option[Service]] =
    findById(path.split('/').filter(_.nonEmpty).head)

  override def getServiceFor(pattern: Regex): Task[Option[Service]] =
    findById(pattern.regex.split('/').filter(_.nonEmpty).head)
}

object ServiceDAOImpl {
  val live: URLayer[MongoCollection[BsonDocument], ServiceDAO[Task]] = ZLayer.fromFunction(new ServiceDAOImpl(_))
}
