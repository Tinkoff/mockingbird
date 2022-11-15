package ru.tinkoff.tcb.mockingbird.dal

import scala.annotation.implicitNotFound

import cats.tagless.autoFunctorK
import com.github.dwickern.macros.NameOf.*
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Indexes.descending
import simulacrum.typeclass

import ru.tinkoff.tcb.mockingbird.model.SourceConfiguration
import ru.tinkoff.tcb.mongo.DAOBase
import ru.tinkoff.tcb.mongo.MongoDAO
import ru.tinkoff.tcb.utils.crypto.AES
import ru.tinkoff.tcb.utils.id.SID

@implicitNotFound("Could not find an instance of SourceConfigurationDAO for ${F}")
@typeclass @autoFunctorK
trait SourceConfigurationDAO[F[_]] extends MongoDAO[F, SourceConfiguration] {
  def getAll: F[Vector[SourceConfiguration]] = findChunk(BsonDocument(), 0, Int.MaxValue)
  def getAllNames: F[Vector[SID[SourceConfiguration]]]
}

object SourceConfigurationDAO {
  /* ======================================================================== */
  /* THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      */
  /* ======================================================================== */

  /**
   * Summon an instance of [[SourceConfigurationDAO]] for `F`.
   */
  @inline def apply[F[_]](implicit instance: SourceConfigurationDAO[F]): SourceConfigurationDAO[F] = instance

  object ops {
    implicit def toAllSourceConfigurationDAOOps[F[_], A](
        target: F[A]
    )(implicit tc: SourceConfigurationDAO[F]): AllOps[F, A] {
      type TypeClassType = SourceConfigurationDAO[F]
    } = new AllOps[F, A] {
      type TypeClassType = SourceConfigurationDAO[F]
      val self: F[A]                       = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  trait Ops[F[_], A] extends Serializable {
    type TypeClassType <: SourceConfigurationDAO[F]
    def self: F[A]
    val typeClassInstance: TypeClassType
  }
  trait AllOps[F[_], A] extends Ops[F, A]
  trait ToSourceConfigurationDAOOps extends Serializable {
    implicit def toSourceConfigurationDAOOps[F[_], A](target: F[A])(implicit tc: SourceConfigurationDAO[F]): Ops[F, A] {
      type TypeClassType = SourceConfigurationDAO[F]
    } = new Ops[F, A] {
      type TypeClassType = SourceConfigurationDAO[F]
      val self: F[A]                       = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToSourceConfigurationDAOOps

  /* ======================================================================== */
  /* END OF SIMULACRUM-MANAGED CODE                                           */
  /* ======================================================================== */

}

class SourceConfigurationDAOImpl(collection: MongoCollection[BsonDocument])(implicit aes: AES)
    extends DAOBase[SourceConfiguration](collection)
    with SourceConfigurationDAO[Task] {
  override def getAllNames: Task[Vector[SID[SourceConfiguration]]] = getAll.map(_.map(_.name))

  def createIndexes: Task[Unit] = createIndex(
    ascending(nameOf[SourceConfiguration](_.service))
  ) *>
    createIndex(
      descending(nameOf[SourceConfiguration](_.created))
    )
}

object SourceConfigurationDAOImpl {
  val live: RLayer[MongoCollection[BsonDocument] & AES, SourceConfigurationDAO[Task]] =
    ZLayer {
      for {
        coll                <- ZIO.service[MongoCollection[BsonDocument]]
        implicit0(aes: AES) <- ZIO.service[AES]
        scd = new SourceConfigurationDAOImpl(coll)
        _ <- scd.createIndexes
      } yield scd
    }
}
