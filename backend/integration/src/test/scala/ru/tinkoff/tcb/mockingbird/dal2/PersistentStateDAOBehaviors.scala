package ru.tinkoff.tcb.mockingbird.dal2

import scala.concurrent.Future

import org.scalatest.funsuite.AsyncFunSuiteLike
import org.scalatest.matchers.should.Matchers

import ru.tinkoff.tcb.mockingbird.test.util.JGen

trait PersistentStateDAOBehaviors[F[_]] extends AsyncFunSuiteLike with Matchers with AsyncCancelableTests {

  implicit def M: Monad[F]
  implicit def fToFuture[T](fwh: F[T]): Future[T]

  def dao: PersistentStateDAO[F]
}

object PersistentStateDAOBehaviors {}

class TestSuite extends org.scalatest.funsuite.AnyFunSuite {
  import org.scalacheck.Gen
  import org.scalacheck.rng.Seed

  test("test #1") {
    (0 to 10).foreach(_ => info(JGen.jObject.pureApply(Gen.Parameters.default, Seed.random()).spaces2))
  }
}
