package ru.tinkoff.tcb.mockingbird.dal

import com.github.dwickern.macros.NameOf.*
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters.exists
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes.*

import ru.tinkoff.tcb.bson.BsonEncoder.ops.*
import ru.tinkoff.tcb.circe.bson.jsonBsonWriter
import ru.tinkoff.tcb.dataaccess.UpdateResult
import ru.tinkoff.tcb.mockingbird.misc.Renderable
import ru.tinkoff.tcb.mockingbird.misc.Renderable.ops.*
import ru.tinkoff.tcb.mockingbird.model.PersistentState
import ru.tinkoff.tcb.mongo.DAOBase
import ru.tinkoff.tcb.mongo.MongoDAO
import ru.tinkoff.tcb.utils.id.SID

trait PersistentStateDAO[F[_]] extends MongoDAO[F, PersistentState] {
  def findBySpec[S: Renderable](spec: S): F[Vector[PersistentState]]

  def upsertBySpec[S: Renderable](id: SID[PersistentState], spec: S): F[UpdateResult]

  def createIndexForDataField(field: String): F[Unit]
}

object PersistentStateDAO

class PersistentStateDAOImpl(collection: MongoCollection[BsonDocument])
    extends DAOBase[PersistentState](collection)
    with PersistentStateDAO[Task] {
  override def findBySpec[S: Renderable](spec: S): Task[Vector[PersistentState]] =
    findChunk(spec.withPrefix(nameOf[PersistentState](_.data)).renderJson.bson.asDocument(), 0, Int.MaxValue)

  override def upsertBySpec[S: Renderable](id: SID[PersistentState], spec: S): Task[UpdateResult] =
    ZIO.clockWith(_.instant).flatMap { now =>
      upsert(
        id,
        PersistentState(id, spec.renderJson, now),
        BsonDocument("$set" -> spec.withPrefix(nameOf[PersistentState](_.data)).renderJson.bson)
      )
    }

  override def createIndexForDataField(field: String): Task[Unit] =
    createIndex(
      ascending(s"${nameOf[PersistentState](_.data)}.$field"),
      IndexOptions().partialFilterExpression(exists(s"${nameOf[PersistentState](_.data)}.$field"))
    )
}

object PersistentStateDAOImpl {
  val live: URLayer[MongoCollection[BsonDocument], PersistentStateDAO[Task]] =
    ZLayer.fromFunction(new PersistentStateDAOImpl(_))
}
