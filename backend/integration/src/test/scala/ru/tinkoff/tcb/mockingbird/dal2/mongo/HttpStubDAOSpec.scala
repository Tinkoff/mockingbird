package ru.tinkoff.tcb.mockingbird.dal2.mongo

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import com.dimafeng.testcontainers.ContainerDef
import com.dimafeng.testcontainers.GenericContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import eu.timepit.refined.auto.*
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineMV
import org.mongodb.scala.MongoClient
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.mockingbird.dal2

class HttpStubDAOSpec
    extends AsyncFunSuite
    with TestContainerForAll
    with BeforeAndAfterEach
    with Matchers
    with dal2.HttpStubDAOSpecBehaviors[Task] {

  val mongoExposedPort = 27017

  private var mongoClient: MongoClient      = _
  private var mongoDb: MongoDatabase        = _
  private var dao_ : dal2.HttpStubDAO[Task] = _

  val M: Monad[Task] = zio.interop.catz.taskConcurrentInstance
  def fToFuture[T](fwh: Task[T]): Future[T] = Unsafe.unsafe { implicit unsafe =>
    Runtime.default.unsafe.runToFuture(fwh)
  }

  def dao: dal2.HttpStubDAO[Task] = dao_

  override val containerDef: ContainerDef = GenericContainer.Def(
    dockerImage = "mongo",
    exposedPorts = Seq(mongoExposedPort),
  )

  override def afterContainersStart(containers: Containers): Unit = {
    super.afterContainersStart(containers)
    val c = containers.asInstanceOf[GenericContainer]
    mongoClient = MongoClient(s"mongodb://${c.containerIpAddress}:${c.mappedPort(mongoExposedPort)}")
    mongoDb = mongoClient.getDatabase(UUID.randomUUID().toString())
  }

  override def beforeContainersStop(containers: Containers): Unit = {
    mongoClient.close()
    super.beforeContainersStop(containers)
  }

  override protected def beforeEach(): Unit = {
    val collection: MongoCollection[BsonDocument] = mongoDb.getCollection(UUID.randomUUID().toString())
    dao_ = Await.result(fToFuture(HttpStubDAOImpl.create(collection)), Duration.Inf)
    super.beforeEach()
  }

  override protected def afterEach(): Unit =
    super.afterEach()

  override val canceledTests: Map[TestName, CancelTestReason] = Map(
    refineMV[NonEmpty]("Получение списка всех заглушек (fetch): фильтр query по pathPattern") ->
      """MongoDB не позволяет искать по вхождению подстроки в регулярное выражение, нет
возможности преобразовать регулярное выражение в обычную строку над которой
возможна операция regexMatch."""
  )
}
