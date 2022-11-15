package ru.tinkoff.tcb.mongo

import scala.jdk.CollectionConverters.*

import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.Observer
import org.mongodb.scala.bson.*
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.changestream.*

import ru.tinkoff.tcb.bson.*
import ru.tinkoff.tcb.bson.BsonEncoder.ops.*
import ru.tinkoff.tcb.criteria.Untyped.*
import ru.tinkoff.tcb.dataaccess.UpdateResult
import ru.tinkoff.tcb.generic.Fields
import ru.tinkoff.tcb.generic.PropSubset
import ru.tinkoff.tcb.generic.RootOptionFields

abstract class DAOBase[T: BsonDecoder: BsonEncoder](
    protected val collection: MongoCollection[BsonDocument]
) extends MongoDAO[Task, T] {
  override def findOne(query: Query): Task[Option[T]] =
    ZIO
      .fromFuture(_ => collection.find(query).first().headOption())
      .flatMap(docOpt => ZIO.fromTry(docOpt.traverse(BsonDecoder[T].fromBson)))

  override def findChunk(
      query: Query,
      offset: Int,
      size: Int,
      sort: Bson*
  ): Task[Vector[T]] =
    ZIO
      .fromFuture(_ =>
        collection
          .find(query)
          .skip(offset)
          .limit(size)
          .sort(collectBsonPatches(sort))
          .foldLeft(Vector.newBuilder[BsonDocument])(_ += _)
          .map(_.result())
          .head()
      )
      .flatMap(docs => ZIO.fromTry(docs.traverse(BsonDecoder[T].fromBson)))

  override def insert(t: T): Task[Int] =
    ZIO.fromFuture(_ => collection.insertOne(t.bson.asDocument()).toFuture()).as(1)

  override def insertMany(ts: Seq[T]): Task[Int] =
    if (ts.nonEmpty)
      ZIO
        .fromFuture(_ => collection.insertMany(ts.map(_.bson.asDocument())).toFuture())
        .as(ts.size)
    else
      ZIO.succeed(0)

  override def update(query: Bson, patches: Bson*): Task[UpdateResult] = update(query, patches)

  override def update(query: Bson, patches: Iterable[Bson]): Task[UpdateResult] =
    ZIO
      .fromFuture(_ => collection.updateOne(query, collectBsonPatches(patches)).head())
      .map(ur => UpdateResult(ur.getMatchedCount, ur.getModifiedCount))

  override def update(entity: T)(implicit rof: RootOptionFields[T]): Task[UpdateResult] = {
    val (entityId, patch) = PatchGenerator.mkPatch(entity)
    val query             = entityId.map(eid => where(_._id === eid))

    query.fold(ZIO.attempt(UpdateResult.empty))(expr => runUpdate(expr.bson.asDocument(), patch))
  }

  override def updateIf(query: Bson, entity: T)(implicit
      rof: RootOptionFields[T]
  ): Task[UpdateResult] = {
    val (entityId, patch) = PatchGenerator.mkPatch(entity)
    val compQuery = entityId
      .map(eid => where(_._id === eid))
      .map(_.bson :+ query.toBsonDocument(classOf[BsonDocument], DEFAULT_CODEC_REGISTRY))

    compQuery.fold(ZIO.attempt(UpdateResult.empty)) { expr =>
      runUpdate(expr.bson.asDocument(), patch)
    }
  }

  override def patch[P: BsonEncoder](
      patch: P
  )(implicit rof: RootOptionFields[P], ps: PropSubset[P, T]): Task[UpdateResult] = {
    val (entityId, patchDoc) = PatchGenerator.mkPatch(patch)
    val query                = entityId.map(eid => where(_._id === eid))

    query.fold(ZIO.attempt(UpdateResult.empty))(expr => runUpdate(expr.bson.asDocument(), patchDoc))
  }

  override def patchIf[P: BsonEncoder](query: Query, patch: P)(implicit
      rof: RootOptionFields[P],
      ps: PropSubset[P, T]
  ): Task[UpdateResult] = {
    val (entityId, patchDoc) = PatchGenerator.mkPatch(patch)
    val compQuery = entityId
      .map(eid => where(_._id === eid))
      .map(_.bson :+ query.toBsonDocument(classOf[BsonDocument], DEFAULT_CODEC_REGISTRY))

    compQuery.fold(ZIO.attempt(UpdateResult.empty)) { expr =>
      runUpdate(expr.bson.asDocument(), patchDoc)
    }
  }

  protected def runUpdate(query: Query, patch: Patch): Task[UpdateResult] =
    ZIO
      .fromFuture(_ => collection.updateOne(query, patch).head())
      .map(ur => UpdateResult(ur.getMatchedCount, ur.getModifiedCount))

  override def delete(query: Bson): Task[Long] =
    ZIO.fromFuture(_ => collection.deleteMany(query).head()).map(_.getDeletedCount)

  protected def collectBsonPatches(patches: Iterable[Bson]): BsonDocument =
    patches
      .map(_.toBsonDocument(classOf[BsonDocument], DEFAULT_CODEC_REGISTRY))
      .flatMap(_.asScala.toList)
      .foldLeft(BsonDocument())(_ +! _)

  override def findOneProjection[P: BsonDecoder](
      query: Bson
  )(implicit fields: Fields[P], ps: PropSubset[P, T]): Task[Option[P]] =
    ZIO
      .fromFuture(_ =>
        collection
          .find(query)
          .projection(BsonDocument(fields.fields.map(_ -> BsonInt32(1))))
          .first()
          .headOption()
      )
      .flatMap(docOpt => ZIO.fromTry(docOpt.traverse(BsonDecoder[P].fromBson)))

  override def findChunkProjection[P: BsonDecoder](
      query: Bson,
      offset: Int,
      size: Int,
      sort: Iterable[Sort]
  )(implicit fields: Fields[P], ps: PropSubset[P, T]): Task[Vector[P]] =
    ZIO
      .fromFuture(_ =>
        collection
          .find(query)
          .projection(BsonDocument(fields.fields.map(_ -> BsonInt32(1))))
          .skip(offset)
          .limit(size)
          .sort(collectBsonPatches(sort))
          .foldLeft(Vector.newBuilder[BsonDocument])(_ += _)
          .map(_.result())
          .head()
      )
      .flatMap(docs => ZIO.fromTry(docs.traverse(BsonDecoder[P].fromBson)))

  override def upsert[Id: BsonEncoder](
      appId: Id,
      create: => T,
      patches: Bson*
  ): Task[UpdateResult] =
    findById(appId)
      .map(_.isDefined)
      .flatMap { found =>
        if (found)
          updateById(appId, patches*)
        else
          insert(create).map(UpdateResult(0, _))
      }

  override def subscribe(consumer: ChangeStreamDocument[T] => Unit): Unit =
    collection
      .watch()
      .fullDocument(FullDocument.UPDATE_LOOKUP)
      .subscribe(new Observer[ChangeStreamDocument[BsonDocument]] {
        override def onNext(result: ChangeStreamDocument[BsonDocument]): Unit =
          consumer(
            new ChangeStreamDocument[T](
              result.getOperationType,
              result.getResumeToken,
              result.getNamespaceDocument,
              result.getDestinationNamespaceDocument,
              BsonDecoder[T].fromBson(result.getFullDocument).get,
              result.getDocumentKey,
              result.getClusterTime,
              result.getUpdateDescription,
              result.getTxnNumber,
              result.getLsid
            )
          )

        override def onError(e: Throwable): Unit =
          subscribe(consumer)

        override def onComplete(): Unit = ()
      })

  override def createIndex(defn: Sort, options: IndexOptions): Task[Unit] =
    ZIO.fromFuture(implicit ec => collection.createIndex(defn, options).head()).unit
}
