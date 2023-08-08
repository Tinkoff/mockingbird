package ru.tinkoff.tcb.mockingbird.dal2.mongo

import java.time.Instant

import com.github.dwickern.macros.NameOf.*
import eu.timepit.refined.auto.*
import mouse.boolean.*
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.BsonDocument
import zio.ZIO

import ru.tinkoff.tcb.criteria.*
import ru.tinkoff.tcb.criteria.Typed.*
import ru.tinkoff.tcb.criteria.Untyped.*
import ru.tinkoff.tcb.dataaccess.UpdateResult
import ru.tinkoff.tcb.mockingbird.api.request.StubPatch
import ru.tinkoff.tcb.mockingbird.dal
import ru.tinkoff.tcb.mockingbird.dal2
import ru.tinkoff.tcb.mockingbird.dal2.model.StubExactlyPath
import ru.tinkoff.tcb.mockingbird.dal2.model.StubFetchParams
import ru.tinkoff.tcb.mockingbird.dal2.model.StubFindParams
import ru.tinkoff.tcb.mockingbird.dal2.model.StubMatchParams
import ru.tinkoff.tcb.mockingbird.dal2.model.StubPathPattern
import ru.tinkoff.tcb.mockingbird.model.HttpStub
import ru.tinkoff.tcb.mockingbird.model.Scope
import ru.tinkoff.tcb.protocol.rof.*
import ru.tinkoff.tcb.utils.id.SID

class HttpStubDAOImpl(collection: MongoCollection[BsonDocument]) extends dal2.HttpStubDAO[Task] {
  private val impl = new dal.HttpStubDAOImpl(collection)

  override def get(id: SID[HttpStub]): Task[Option[HttpStub]] = impl.findById(id)

  override def insert(stub: HttpStub): Task[Long] = impl.insert(stub).map(x => x)

  override def update(patch: StubPatch): Task[UpdateResult] = impl.patch(patch)

  override def delete(id: SID[HttpStub]): Task[Long] = impl.deleteById(id)

  override def deleteExpired(threshold: Instant): Task[Long] =
    impl.delete(
      prop[HttpStub](_.scope).in[Scope](Scope.Ephemeral, Scope.Countdown) && prop[HttpStub](_.created) < threshold
    )

  override def deleteDepleted(): Task[Long] =
    impl.delete(prop[HttpStub](_.scope) === Scope.Countdown.asInstanceOf[Scope] && prop[HttpStub](_.times) <= Option(0))

  override def find(params: StubFindParams): Task[Vector[HttpStub]] =
    impl.findChunk(
      prop[HttpStub](_.method) === params.method &&
        (params.path match {
          case StubExactlyPath(p)  => prop[HttpStub](_.path) === Option(p.value)
          case StubPathPattern(pp) => prop[HttpStub](_.pathPattern) === Option(pp)
        }) &&
        prop[HttpStub](_.scope) === params.scope &&
        prop[HttpStub](_.times) > Option(0),
      0,
      Int.MaxValue
    )

  override def findMatch(params: StubMatchParams): Task[Vector[HttpStub]] = {
    val pathPatternExpr = Expression[HttpStub](
      None,
      "$expr" -> BsonDocument(
        "$regexMatch" -> BsonDocument(
          "input" -> params.path,
          "regex" -> s"$$${nameOf[HttpStub](_.pathPattern)}"
        )
      )
    )
    val condition0 = prop[HttpStub](_.method) === params.method &&
      (prop[HttpStub](_.path) ==@ params.path || pathPatternExpr) &&
      prop[HttpStub](_.scope) === params.scope
    val condition = (params.scope == Scope.Countdown).fold(condition0 && prop[HttpStub](_.times) > Option(0), condition0)
    impl.findChunk(condition, 0, Int.MaxValue)
  }

  override def fetch(params: StubFetchParams): Task[Vector[HttpStub]] = {
    var queryDoc: Expression[Any] =
      prop[HttpStub](_.scope) =/= Scope.Countdown.asInstanceOf[Scope] || prop[HttpStub](_.times) > Option(0)

    queryDoc = params.query.fold(queryDoc) { qs =>
      val q = where(_._id === qs) ||
        prop[HttpStub](_.name).regex(qs, "i") ||
        prop[HttpStub](_.path).regex(qs, "i") ||
        prop[HttpStub](_.pathPattern).regex(qs, "i")
      queryDoc && q
    }

    queryDoc = params.service.fold(queryDoc) { service =>
      queryDoc && (prop[HttpStub](_.serviceSuffix) === service)
    }

    if (params.labels.nonEmpty) {
      queryDoc = queryDoc && (prop[HttpStub](_.labels).containsAll(params.labels))
    }

    impl.findChunk(queryDoc, params.page * params.count, params.count, prop[HttpStub](_.created).sort(Desc))
  }

  override def incTimesById(id: SID[HttpStub], value: Int): Task[UpdateResult] =
    impl.updateById(id, prop[HttpStub](_.times).inc(value))

  def createIndexes(): Task[Unit] = impl.createIndexes
}

object HttpStubDAOImpl {
  def create(collection: MongoCollection[BsonDocument]): Task[dal2.HttpStubDAO[Task]] =
    for {
      _ <- ZIO.unit
      sd = new HttpStubDAOImpl(collection)
      _ <- sd.createIndexes()
    } yield sd

  val live: RLayer[MongoCollection[BsonDocument], dal2.HttpStubDAO[Task]] = ZLayer {
    for {
      mc <- ZIO.service[MongoCollection[BsonDocument]]
      sd <- create(mc)
    } yield sd
  }
}
