package ru.tinkoff.tcb.mockingbird.dal

import scala.annotation.implicitNotFound

import cats.tagless.autoFunctorK
import com.github.dwickern.macros.NameOf.*
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Indexes.*
import simulacrum.typeclass

import ru.tinkoff.tcb.mockingbird.model.GrpcStub
import ru.tinkoff.tcb.mongo.DAOBase
import ru.tinkoff.tcb.mongo.MongoDAO

@implicitNotFound("Could not find an instance of GrpcStubDAO for ${F}")
@typeclass @autoFunctorK
trait GrpcStubDAO[F[_]] extends MongoDAO[F, GrpcStub]

object GrpcStubDAO {
  /* ======================================================================== */
  /* THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      */
  /* ======================================================================== */

  /**
   * Summon an instance of [[GrpcStubDAO]] for `F`.
   */
  @inline def apply[F[_]](implicit instance: GrpcStubDAO[F]): GrpcStubDAO[F] = instance

  object ops {
    implicit def toAllGrpcStubDAOOps[F[_], A](target: F[A])(implicit tc: GrpcStubDAO[F]): AllOps[F, A] {
      type TypeClassType = GrpcStubDAO[F]
    } = new AllOps[F, A] {
      type TypeClassType = GrpcStubDAO[F]
      val self: F[A]                       = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  trait Ops[F[_], A] extends Serializable {
    type TypeClassType <: GrpcStubDAO[F]
    def self: F[A]
    val typeClassInstance: TypeClassType
  }
  trait AllOps[F[_], A] extends Ops[F, A]
  trait ToGrpcStubDAOOps extends Serializable {
    implicit def toGrpcStubDAOOps[F[_], A](target: F[A])(implicit tc: GrpcStubDAO[F]): Ops[F, A] {
      type TypeClassType = GrpcStubDAO[F]
    } = new Ops[F, A] {
      type TypeClassType = GrpcStubDAO[F]
      val self: F[A]                       = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToGrpcStubDAOOps

  /* ======================================================================== */
  /* END OF SIMULACRUM-MANAGED CODE                                           */
  /* ======================================================================== */

}

class GrpcStubDAOImpl(collection: MongoCollection[BsonDocument])
    extends DAOBase[GrpcStub](collection)
    with GrpcStubDAO[Task] {
  def createIndexes: Task[Unit] = createIndex(
    ascending(nameOf[GrpcStub](_.methodName), nameOf[GrpcStub](_.scope))
  ) *> createIndex(
    descending(nameOf[GrpcStub](_.created))
  ) *> createIndex(
    ascending(nameOf[GrpcStub](_.service))
  ) *> createIndex(
    ascending(nameOf[GrpcStub](_.labels))
  )
}

object GrpcStubDAOImpl {
  val live = ZLayer {
    for {
      mc <- ZIO.service[MongoCollection[BsonDocument]]
      sd = new GrpcStubDAOImpl(mc)
      _ <- sd.createIndexes
    } yield sd.asInstanceOf[GrpcStubDAO[Task]]
  }
}
