package ru.tinkoff.tcb.mockingbird.dal

import scala.annotation.implicitNotFound

import cats.tagless.autoFunctorK
import com.github.dwickern.macros.NameOf.*
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Indexes.descending
import simulacrum.typeclass

import ru.tinkoff.tcb.mockingbird.model.DestinationConfiguration
import ru.tinkoff.tcb.mongo.DAOBase
import ru.tinkoff.tcb.mongo.MongoDAO
import ru.tinkoff.tcb.utils.crypto.AES
import ru.tinkoff.tcb.utils.id.SID

@implicitNotFound("Could not find an instance of DestinationConfigurationDAO for ${F}")
@typeclass @autoFunctorK
trait DestinationConfigurationDAO[F[_]] extends MongoDAO[F, DestinationConfiguration] {
  def getAll: F[Vector[DestinationConfiguration]] = findChunk(BsonDocument(), 0, Int.MaxValue)
  def getAllNames: F[Vector[SID[DestinationConfiguration]]]
}

object DestinationConfigurationDAO {
  /* ======================================================================== */
  /* THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      */
  /* ======================================================================== */

  /**
   * Summon an instance of [[DestinationConfigurationDAO]] for `F`.
   */
  @inline def apply[F[_]](implicit instance: DestinationConfigurationDAO[F]): DestinationConfigurationDAO[F] = instance

  object ops {
    implicit def toAllDestinationConfigurationDAOOps[F[_], A](
        target: F[A]
    )(implicit tc: DestinationConfigurationDAO[F]): AllOps[F, A] {
      type TypeClassType = DestinationConfigurationDAO[F]
    } = new AllOps[F, A] {
      type TypeClassType = DestinationConfigurationDAO[F]
      val self: F[A]                       = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  trait Ops[F[_], A] extends Serializable {
    type TypeClassType <: DestinationConfigurationDAO[F]
    def self: F[A]
    val typeClassInstance: TypeClassType
  }
  trait AllOps[F[_], A] extends Ops[F, A]
  trait ToDestinationConfigurationDAOOps extends Serializable {
    implicit def toDestinationConfigurationDAOOps[F[_], A](
        target: F[A]
    )(implicit tc: DestinationConfigurationDAO[F]): Ops[F, A] {
      type TypeClassType = DestinationConfigurationDAO[F]
    } = new Ops[F, A] {
      type TypeClassType = DestinationConfigurationDAO[F]
      val self: F[A]                       = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToDestinationConfigurationDAOOps

  /* ======================================================================== */
  /* END OF SIMULACRUM-MANAGED CODE                                           */
  /* ======================================================================== */

}

class DestinationConfigurationDAOImpl(collection: MongoCollection[BsonDocument])(implicit aes: AES)
    extends DAOBase[DestinationConfiguration](collection)
    with DestinationConfigurationDAO[Task] {
  override def getAllNames: Task[Vector[SID[DestinationConfiguration]]] = getAll.map(_.map(_.name))

  def createIndexes: Task[Unit] = createIndex(
    ascending(nameOf[DestinationConfiguration](_.service))
  ) *>
    createIndex(descending(nameOf[DestinationConfiguration](_.created)))
}

object DestinationConfigurationDAOImpl {
  val live: RLayer[MongoCollection[BsonDocument] & AES, DestinationConfigurationDAO[Task]] =
    ZLayer {
      for {
        coll                <- ZIO.service[MongoCollection[BsonDocument]]
        implicit0(aes: AES) <- ZIO.service[AES]
        dcd = new DestinationConfigurationDAOImpl(coll)
        _ <- dcd.createIndexes
      } yield dcd
    }
}
