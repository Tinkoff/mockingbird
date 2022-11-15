package ru.tinkoff.tcb.mockingbird.dal

import scala.annotation.implicitNotFound

import cats.tagless.autoFunctorK
import com.github.dwickern.macros.NameOf.nameOf
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes.ascending
import simulacrum.typeclass

import ru.tinkoff.tcb.criteria.*
import ru.tinkoff.tcb.criteria.Typed.*
import ru.tinkoff.tcb.dataaccess.UpdateResult
import ru.tinkoff.tcb.mockingbird.model.Label
import ru.tinkoff.tcb.mongo.DAOBase
import ru.tinkoff.tcb.mongo.MongoDAO
import ru.tinkoff.tcb.utils.id.SID

@implicitNotFound("Could not find an instance of LabelDAO for ${F}")
@typeclass @autoFunctorK
trait LabelDAO[F[_]] extends MongoDAO[F, Label] {
  def ensureLabels(service: String, labels: Vector[String]): F[UpdateResult]
}

object LabelDAO {
  /* ======================================================================== */
  /* THE FOLLOWING CODE IS MANAGED BY SIMULACRUM; PLEASE DO NOT EDIT!!!!      */
  /* ======================================================================== */

  /**
   * Summon an instance of [[LabelDAO]] for `F`.
   */
  @inline def apply[F[_]](implicit instance: LabelDAO[F]): LabelDAO[F] = instance

  object ops {
    implicit def toAllLabelDAOOps[F[_], A](target: F[A])(implicit tc: LabelDAO[F]): AllOps[F, A] {
      type TypeClassType = LabelDAO[F]
    } = new AllOps[F, A] {
      type TypeClassType = LabelDAO[F]
      val self: F[A]                       = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  trait Ops[F[_], A] extends Serializable {
    type TypeClassType <: LabelDAO[F]
    def self: F[A]
    val typeClassInstance: TypeClassType
  }
  trait AllOps[F[_], A] extends Ops[F, A]
  trait ToLabelDAOOps extends Serializable {
    implicit def toLabelDAOOps[F[_], A](target: F[A])(implicit tc: LabelDAO[F]): Ops[F, A] {
      type TypeClassType = LabelDAO[F]
    } = new Ops[F, A] {
      type TypeClassType = LabelDAO[F]
      val self: F[A]                       = target
      val typeClassInstance: TypeClassType = tc
    }
  }
  object nonInheritedOps extends ToLabelDAOOps

  /* ======================================================================== */
  /* END OF SIMULACRUM-MANAGED CODE                                           */
  /* ======================================================================== */

}

class LabelDAOImpl(collection: MongoCollection[BsonDocument]) extends DAOBase[Label](collection) with LabelDAO[Task] {
  def createIndexes: Task[Unit] =
    createIndex(
      ascending(nameOf[Label](_.serviceSuffix)),
    ) *> createIndex(
      ascending(nameOf[Label](_.serviceSuffix), nameOf[Label](_.label)),
      IndexOptions().unique(true)
    )

  override def ensureLabels(service: String, labels: Vector[String]): Task[UpdateResult] =
    if (labels.isEmpty) ZIO.attempt(UpdateResult.empty)
    else {
      for {
        existing <- findChunk(
          prop[Label](_.serviceSuffix) === service && prop[Label](_.label).in(labels),
          0,
          labels.size
        )
        existingLabels = existing.map(_.label).toSet
        labelsToCreate = labels.filterNot(existingLabels)
        inserted <- insertMany(labelsToCreate.map(Label(SID.random[Label], service, _)))
      } yield UpdateResult(inserted)
    }
}

object LabelDAOImpl {
  val live = ZLayer {
    for {
      mc <- ZIO.service[MongoCollection[BsonDocument]]
      sd = new LabelDAOImpl(mc)
      _ <- sd.createIndexes
    } yield sd.asInstanceOf[LabelDAO[Task]]
  }
}
