package ru.tinkoff.tcb.mockingbird.dal2

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Random
import scala.util.matching.Regex

import cats.Monad
import eu.timepit.refined.auto.*
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.refineV
import io.circe.Json
import io.circe.syntax.*
import io.scalaland.chimney.dsl.*
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalacheck.rng.Seed
import org.scalatest.EitherValues.*
import org.scalatest.OptionValues.*
import org.scalatest.funsuite.AsyncFunSuiteLike
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import ru.tinkoff.tcb.dataaccess.UpdateResult
import ru.tinkoff.tcb.mockingbird.api.request.StubPatch
import ru.tinkoff.tcb.mockingbird.dal2.model.*
import ru.tinkoff.tcb.mockingbird.model.*
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.utils.id.SID

@SuppressWarnings(
  Array(
    "scalafix:DisableSyntax.mapAs"
  )
)
trait HttpStubDAOSpecBehaviors[F[_]]
    extends AsyncFunSuiteLike
    with Matchers
    with ScalaCheckDrivenPropertyChecks
    with AsyncCancelableTests {

  // В некоторых случаях, можно встретить, что заглушки сравниваются как Json,
  // после вызова asJson. Это связано с тем, что поле pathPattern имеет тип Option[Regex],
  // а для Regex метод equals не сравнивает инстансы по значению, а только по ссылке.
  // Можно было бы переопределеить org.scalatic.Equiality и использовать shouldEqual
  // вместо shouldBe, но вывод Equality не является рекурсивным, поэтому переопределенный
  // имплисит не будет использоваться ни для вложенных полей, ни для элементов коллеций.
  // см. https://github.com/scalatest/scalatest/issues/917#issuecomment-227376714

  implicit def M: Monad[F]
  implicit def fToFuture[T](fwh: F[T]): Future[T]

  def dao: HttpStubDAO[F]

  import HttpStubDAOSpecBehaviors.*

  implicit override def executionContext: ExecutionContext = ExecutionContext.Implicits.global

  test("Сохранить, прочитать, обновить, удалить заглушку") {
    val stub  = genHttpStub()
    val patch = genHttpStubPatch(stub.id, stub.serviceSuffix)

    val updStub = patch
      .into[HttpStub]
      .withFieldConst(_.created, stub.created)
      .withFieldConst(_.serviceSuffix, stub.serviceSuffix)
      .transform

    for {
      ir <- dao.insert(stub)
      _ = ir shouldBe 1

      obtained <- dao.get(stub.id)
      _ = obtained.map(_.asJson) shouldBe Some(stub.asJson)

      ur <- dao.update(patch)
      _ = ur.successful shouldBe true

      obtained <- dao.get(stub.id)
      _ = obtained.map(_.asJson) shouldBe Some(updStub.asJson)

      dr <- dao.delete(stub.id)
      _ = dr shouldBe 1

      obtained3 <- dao.get(stub.id)
    } yield obtained3 shouldBe empty
  }

  test("Сохранить, прочитать, обновить, удалить заглушку v2") {
    val stubAndUpd: Gen[(HttpStub, StubPatch)] =
      for {
        stub  <- gen.httpStub
        patch <- gen.stubPatch(stub.id, stub.serviceSuffix)
      } yield (stub, patch)

    implicit val arbStubAndUpd: Arbitrary[(HttpStub, StubPatch)] = Arbitrary(stubAndUpd)

    forAll { (sp: (HttpStub, StubPatch)) =>
      val (stub, patch) = sp
      val updStub = patch
        .into[HttpStub]
        .withFieldConst(_.created, stub.created)
        .withFieldConst(_.serviceSuffix, stub.serviceSuffix)
        .transform

      val result = for {
        ir <- dao.insert(stub)
        _ = ir shouldBe 1

        obtained <- dao.get(stub.id)
        _ = obtained.map(_.asJson) shouldBe Some(stub.asJson)

        ur <- dao.update(patch)
        _ = ur.successful shouldBe true

        obtained <- dao.get(stub.id)
        _ = obtained.map(_.asJson) shouldBe Some(updStub.asJson)

        dr <- dao.delete(stub.id)
        _ = dr shouldBe 1

        obtained3 <- dao.get(stub.id)
      } yield obtained3 shouldBe empty

      Await.result(result, Duration.Inf)
    }
  }

  test("Получение списка всех заглушек (fetch): без фильтров") {
    val now   = Instant.now().truncatedTo(ChronoUnit.MILLIS) // scalafix:ok
    val stubs = (1 to 13).map(i => HttpStubDAOSpecBehaviors.genHttpStub(_.copy(created = now.minusMillis(i)))).toVector

    // Заглушки возвращаются отсортированными по дате создания в обратном
    // порядке, т.е. те, что были созданы позже - в конце
    val expected = stubs.sortBy(_.created)(Ordering[Instant].reverse)

    for {
      _  <- Traverse[Vector].traverse(Random.shuffle(stubs))(dao.insert)
      p1 <- dao.fetch(StubFetchParams(page = 0, query = None, service = None, labels = Seq.empty, count = 5))
      _ = p1 should have size 5
      p2 <- dao.fetch(StubFetchParams(page = 1, query = None, service = None, labels = Seq.empty, count = 5))
      _ = p2 should have size 5
      p3 <- dao.fetch(StubFetchParams(page = 2, query = None, service = None, labels = Seq.empty, count = 5))
      _ = p3 should have size 3
    } yield (p1 ++ p2 ++ p3).map(_.asJson) shouldBe expected.map(_.asJson)
  }

  test("Получение списка всех заглушек (fetch): фильтр query == ID") {
    val stubs = (1 to 5).toVector.map(_ => HttpStubDAOSpecBehaviors.genHttpStub())

    for {
      _ <- Traverse[Vector].traverse(stubs)(dao.insert)
      expected = stubs(2)
      obtained <- dao.fetch(
        StubFetchParams(page = 0, query = Some(expected.id), service = None, labels = Seq.empty, count = 5)
      )
    } yield obtained.map(_.asJson) shouldBe Vector(expected.asJson)
  }

  test("Получение списка всех заглушек (fetch): фильтр query по name") {
    val partName = "desired"
    val desiredStubs =
      (1 to 3).toVector.map(_ => HttpStubDAOSpecBehaviors.genHttpStub(s => s.copy(name = s.name + partName)))

    val otherStubs = (1 to 10).toVector.map(_ => genHttpStub())
    val stubs      = Random.shuffle(desiredStubs ++ otherStubs)
    val expected   = desiredStubs.sortBy(_.id)

    for {
      _ <- Traverse[Vector].traverse(stubs)(dao.insert)
      obtained <- dao.fetch(
        StubFetchParams(page = 0, query = Some(partName), service = None, labels = Seq.empty, count = 5)
      )
    } yield obtained.sortBy(_.id).map(_.asJson) shouldBe expected.map(_.asJson)
  }

  test("Получение списка всех заглушек (fetch): фильтр query по path") {
    val partPath = "desired"
    val desiredStubs =
      (1 to 3).toVector.map(_ =>
        HttpStubDAOSpecBehaviors.genHttpStub(s =>
          s.copy(
            path = Some(s"/${s.serviceSuffix}/api/$partPath/${genAlphanum(10)}"),
            pathPattern = None
          )
        )
      )
    val otherStubs = (1 to 10).toVector.map(_ => genHttpStub())
    val stubs      = Random.shuffle(desiredStubs ++ otherStubs)
    val expected   = desiredStubs.sortBy(_.id)

    for {
      _ <- Traverse[Vector].traverse(stubs)(dao.insert)
      obtained <- dao.fetch(
        StubFetchParams(page = 0, query = Some(partPath), service = None, labels = Seq.empty, count = 5)
      )
    } yield obtained.sortBy(_.id).map(_.asJson) shouldBe expected.map(_.asJson)
  }

  test("Получение списка всех заглушек (fetch): фильтр query по pathPattern") {
    val partPath = "desired"
    val desiredStubs =
      (1 to 3).toVector.map(_ =>
        HttpStubDAOSpecBehaviors.genHttpStub(s =>
          s.copy(
            path = None,
            pathPattern = Some(s"/${s.serviceSuffix}/api/$partPath/${genAlphanum(10)}/[0-9]+".r)
          )
        )
      )
    val otherStubs = (1 to 10).toVector.map(_ => genHttpStub())
    val stubs      = Random.shuffle(desiredStubs ++ otherStubs)
    val expected   = desiredStubs.sortBy(_.id)

    for {
      _ <- Traverse[Vector].traverse(stubs)(dao.insert)
      obtained <- dao.fetch(
        StubFetchParams(page = 0, query = Some(partPath), service = None, labels = Seq.empty, count = 5)
      )
    } yield obtained.sortBy(_.id).map(_.asJson) shouldBe expected.map(_.asJson)
  }

  test("Получение списка всех заглушек (fetch): фильтр сервис") {
    val serviceName = "svc1"
    val desiredStubs =
      (1 to 7).toVector.map(_ => genHttpStub(_.copy(serviceSuffix = serviceName)))
    val otherStubs = (1 to 10).toVector.map(_ => genHttpStub())
    val stubs      = Random.shuffle(desiredStubs ++ otherStubs)
    val expected   = desiredStubs.sortBy(_.id)

    for {
      _ <- Traverse[Vector].traverse(stubs)(dao.insert)

      p1 <- dao.fetch(
        StubFetchParams(page = 0, query = None, service = Some(serviceName), labels = Seq.empty, count = 5)
      )
      _ = p1 should have size 5

      p2 <- dao.fetch(
        StubFetchParams(page = 1, query = None, service = Some(serviceName), labels = Seq.empty, count = 5)
      )
      _ = p2 should have size 2
    } yield (p1 ++ p2).sortBy(_.id).map(_.asJson) shouldBe expected.map(_.asJson)
  }

  test("Получение списка всех заглушек (fetch): фильтр лейблы") {
    val desiredStubs = Vector(
      genHttpStub(_.copy(labels = Seq("l1", "l2"))),
      genHttpStub(_.copy(labels = Seq("l2", "l1"))),
      genHttpStub(_.copy(labels = Seq("l1", "l3", "l2"))),
    )

    val otherStubs = Vector(
      genHttpStub(_.copy(labels = Seq("l1"))),
      genHttpStub(_.copy(labels = Seq("l2"))),
      genHttpStub(),
      genHttpStub(),
    )
    val stubs    = Random.shuffle(desiredStubs ++ otherStubs)
    val expected = desiredStubs.sortBy(_.id)

    for {
      _ <- Traverse[Vector].traverse(stubs)(dao.insert)

      obtained <- dao.fetch(
        StubFetchParams(page = 0, query = None, service = None, labels = Seq("l1", "l2"), count = 5)
      )
    } yield obtained.sortBy(_.id).map(_.asJson) shouldBe expected.map(_.asJson)
  }

  test("Поиск заглушек (find): указан path") {
    val serviceName = "svc1"
    val scope       = genScope()
    val path        = refineV[NonEmpty](s"/$serviceName/api/obj").value
    val method      = HttpMethod.Get

    val desired =
      (1 to 2).toVector.map(_ =>
        genHttpStub(
          _.copy(
            scope = scope,
            serviceSuffix = serviceName,
            path = Some(path),
            pathPattern = None,
            method = method,
            times = Some(1),
          )
        )
      )

    val likeDesired = Vector(
      // Метод find возвращает только заглушки с times > 0
      genHttpStub(
        _.copy(
          scope = scope,
          serviceSuffix = serviceName,
          path = Some(path),
          pathPattern = None,
          method = method,
          times = Some(0),
        )
      ),
      // Сейчас поиск идет для path, поэтому с таким же pathPattern должен пропустить
      genHttpStub(
        _.copy(
          scope = scope,
          serviceSuffix = serviceName,
          path = None,
          pathPattern = Some(new Regex(path)),
          method = method,
          times = Some(1),
        )
      )
    )

    val other    = (1 to 3).toVector.map(_ => genHttpStub())
    val stubs    = Random.shuffle(desired ++ likeDesired ++ other)
    val expected = desired.sortBy(_.id)

    for {
      _ <- Traverse[Vector].traverse(stubs)(dao.insert)
      obtained <- dao.find(
        StubFindParams(scope = scope, path = StubExactlyPath(path), method = method)
      )
    } yield obtained.sortBy(_.id).map(_.asJson) shouldBe expected.map(_.asJson)
  }

  test("Поиск заглушек (find): указан pathPattern") {
    val serviceName = "svc2"
    val scope       = genScope()
    val pathPattern = s"/$serviceName/api/obj/(?<type>[A-z][A-z0-9]+)/[0-9]+".r
    val method      = HttpMethod.Get

    val desired =
      (1 to 2).toVector.map(_ =>
        genHttpStub(
          _.copy(
            scope = scope,
            serviceSuffix = serviceName,
            path = None,
            pathPattern = Some(pathPattern),
            method = method,
            times = Some(1),
          )
        )
      )

    val likeDesired = Vector(
      // Метод find возвращает только заглушки с times > 0
      genHttpStub(
        _.copy(
          scope = scope,
          serviceSuffix = serviceName,
          path = None,
          pathPattern = Some(pathPattern),
          method = method,
          times = Some(0),
        )
      ),
      // Сейчас поиск идет для pathPattern, поэтому с таким же path пропускаем
      genHttpStub(
        _.copy(
          scope = scope,
          serviceSuffix = serviceName,
          path = Some(pathPattern.toString),
          pathPattern = None,
          method = method,
          times = Some(1),
        )
      )
    )

    val other    = (1 to 3).toVector.map(_ => genHttpStub())
    val stubs    = Random.shuffle(desired ++ likeDesired ++ other)
    val expected = desired.sortBy(_.id)

    for {
      _ <- Traverse[Vector].traverse(stubs)(dao.insert)
      obtained <- dao.find(
        StubFindParams(scope = scope, path = StubPathPattern(pathPattern), method = method)
      )
    } yield obtained.sortBy(_.id).map(_.asJson) shouldBe expected.map(_.asJson)
  }

  test("Поисх подходящих заглушек (findMatch)") {
    val desiredStubs = Vector(
      genHttpStub(_.copy(scope = Scope.Countdown, method = HttpMethod.Get, path = Some("/api/obj/123"))),
      genHttpStub(_.copy(scope = Scope.Countdown, method = HttpMethod.Get, path = Some("/api/obj/123"))),
      genHttpStub(_.copy(scope = Scope.Countdown, method = HttpMethod.Get, pathPattern = Some("/api/obj/[0-9]+".r))),
      genHttpStub(
        _.copy(
          scope = Scope.Countdown,
          method = HttpMethod.Get,
          pathPattern = Some("/api/(?<obj>[a-z]+)/(?<id>[0-9]+)".r)
        )
      ),
    )

    val otherStubs =
      desiredStubs.map(_.copy(scope = Scope.Persistent, id = SID.random[HttpStub])) ++
        desiredStubs.map(_.copy(method = HttpMethod.Post, id = SID.random[HttpStub])) ++
        desiredStubs.map(_.copy(times = Some(0), id = SID.random[HttpStub])) ++
        (1 to 10).toVector.map(_ => genHttpStub())

    val stubs    = Random.shuffle(desiredStubs ++ otherStubs)
    val expected = desiredStubs.sortBy(_.id)

    for {
      _ <- Traverse[Vector].traverse(stubs)(dao.insert)

      obtained <- dao.findMatch(
        StubMatchParams(scope = Scope.Countdown, path = "/api/obj/123", method = HttpMethod.Get)
      )
    } yield obtained.sortBy(_.id).map(_.asJson) shouldBe expected.map(_.asJson)
  }

  test("Удаление заглушек с истекшей датой (deleteExpired)") {
    val threshold = Instant.now().truncatedTo(ChronoUnit.MILLIS) // scalafix:ok
    val created   = threshold.minusMillis(1)

    val expired =
      (1 to 3).map(_ => genHttpStub(_.copy(scope = Scope.Countdown, created = created))).toVector ++
        (1 to 3).map(_ => genHttpStub(_.copy(scope = Scope.Ephemeral, created = created))).toVector

    val mustRest =
      (1 to 3).map(_ => genHttpStub(_.copy(scope = Scope.Persistent, created = created))).toVector ++
        (1 to 3).map(_ => genHttpStub(_.copy(scope = Scope.Ephemeral, created = threshold))).toVector
    (1 to 3).map(_ => genHttpStub(_.copy(scope = Scope.Countdown, created = threshold))).toVector

    val stubs    = Random.shuffle(expired ++ mustRest)
    val expected = mustRest.sortBy(_.id)

    for {
      _ <- Traverse[Vector].traverse(stubs)(dao.insert)

      wasDeleted <- dao.deleteExpired(threshold)
      _ = wasDeleted shouldBe expired.size

      obtained <- dao.fetch(
        StubFetchParams(
          page = 0,
          query = None,
          service = None,
          labels = Seq.empty,
          count = refineV[Positive](stubs.size).value
        )
      )
    } yield obtained.sortBy(_.id).map(_.asJson) shouldBe expected.map(_.asJson)
  }

  test("Удаление отработанных countdown заглушек (deleteDepleted)") {
    val depleted =
      (1 to 5).map(_ => genHttpStub(_.copy(scope = Scope.Countdown, times = Some(0)))).toVector ++
        (1 to 5).map(_ => genHttpStub(_.copy(scope = Scope.Countdown, times = Some(-1)))).toVector

    val mustRest =
      (1 to 5).map(_ => genHttpStub(_.copy(scope = Scope.Persistent, times = Some(0)))).toVector ++
        (1 to 5).map(_ => genHttpStub(_.copy(scope = Scope.Ephemeral, times = Some(0)))).toVector

    val stubs    = Random.shuffle(depleted ++ mustRest)
    val expected = mustRest.sortBy(_.id)

    for {
      _ <- Traverse[Vector].traverse(stubs)(dao.insert)

      wasDeleted <- dao.deleteDepleted()
      _ = wasDeleted shouldBe depleted.size

      obtained <- dao.fetch(
        StubFetchParams(
          page = 0,
          query = None,
          service = None,
          labels = Seq.empty,
          count = refineV[Positive](stubs.size).value
        )
      )
    } yield obtained.sortBy(_.id).map(_.asJson) shouldBe expected.map(_.asJson)
  }

  test("Инкремент поля times заглушки (incTimesById)") {
    val stub = genHttpStub(_.copy(times = Some(2)))

    for {
      _ <- dao.insert(stub)

      res <- dao.incTimesById(stub.id, -1)
      _ = res shouldBe UpdateResult(1, 1)

      obtained <- dao.get(stub.id)
    } yield obtained.value.times shouldBe Some(1)
  }
}

object HttpStubDAOSpecBehaviors {
  def genAlphanum(length: Int): String =
    Random.alphanumeric.take(length).mkString

  def genScope(): Scope =
    gen.scope.pureApply(Gen.Parameters.default, Seed.random())

  def genHttpStub(m: HttpStub => HttpStub = identity): HttpStub =
    gen.httpStub.map(m).pureApply(Gen.Parameters.default, Seed.random())

  def genHttpStubPatch(id: SID[HttpStub], service: String, m: StubPatch => StubPatch = identity): StubPatch =
    gen.stubPatch(id, service).map(m).pureApply(Gen.Parameters.default, Seed.random())

  object gen {
    val scope: Gen[Scope] = Gen.oneOf(Scope.values)

    def sid[T]: Gen[SID[T]] = Gen.uuid.map(u => SID[T](u.toString))

    def serviceSuffix: Gen[String] = Gen.alphaNumStr.map(s => "svc-" ++ s.take(5).toLowerCase())

    def method: Gen[HttpMethod] = Gen.oneOf(HttpMethod.values)

    def path(service: String): Gen[String] = {
      val partGen = Gen.alphaNumStr.map(_.take(7).toLowerCase())
      Gen.containerOfN[Seq, String](2, partGen).map { ss =>
        val parts = ss.filter(_.nonEmpty)
        parts.mkString(s"/$service/", "/", "/")
      }
    }

    def pathPattern(service: String): Gen[Regex] =
      path(service).map(p => new Regex(p ++ "[A-z0-9]+"))

    def times(scope: Scope): Gen[Option[Int]] =
      if (scope == Scope.Countdown) Gen.choose[Int](1, 10).map(Some(_))
      else Gen.const(Some(1))

    /**
     * A generator that generates an Instant in range [now - 7 days, now + 2 days]
     */
    val instant: Gen[Instant] = {
      val up   = 2.days.toMillis()
      val down = -7.days.toMillis()
      Gen
        .choose(down, up)
        .map(d => Instant.now().truncatedTo(ChronoUnit.MILLIS).plusMillis(d)) // scalafix:ok
    }

    val httpStub: Gen[HttpStub] = for {
      id            <- sid[HttpStub]
      created       <- instant
      scope         <- scope
      times         <- times(scope)
      service       <- serviceSuffix
      name          <- Gen.asciiPrintableStr
      method        <- method
      pathOrPattern <- Gen.either(path(service), pathPattern(service))
      labelsCount   <- Gen.choose(0, 5)
      labels        <- Gen.containerOfN[Seq, String](labelsCount, Gen.alphaNumStr.map(_.take(5).toLowerCase()))
    } yield HttpStub(
      id = id,
      created = created,
      scope = scope,
      times = times,
      serviceSuffix = service,
      name = name,
      method = method,
      path = pathOrPattern.left.toOption,
      pathPattern = pathOrPattern.toOption,
      seed = None,
      state = None,
      request = RequestWithoutBody(
        headers = Map.empty,
        query = Map(JsonOptic.forPath(genAlphanum(3)) -> Map(Keyword.Equals -> Json.fromString(genAlphanum(4))))
      ),
      persist = None,
      response = RawResponse(code = 200, headers = Map.empty, body = "OK", delay = None),
      callback = None,
      labels = labels,
    )

    def stubPatch(id: SID[HttpStub], service: String): Gen[StubPatch] = for {
      scope         <- scope
      times         <- times(scope)
      name          <- Gen.asciiPrintableStr
      method        <- method
      pathOrPattern <- Gen.either(path(service), pathPattern(service))
      labelsCount   <- Gen.choose(0, 5)
      labels        <- Gen.containerOfN[Seq, String](labelsCount, Gen.alphaNumStr.map(_.take(5).toLowerCase()))
    } yield StubPatch(
      id = id,
      scope = scope,
      times = times,
      name = name,
      method = method,
      path = pathOrPattern.left.toOption,
      pathPattern = pathOrPattern.toOption,
      seed = None,
      state = None,
      request = RequestWithoutBody(
        headers = Map.empty,
        query = Map(JsonOptic.forPath(genAlphanum(3)) -> Map(Keyword.Equals -> Json.fromString(genAlphanum(4))))
      ),
      persist = None,
      response = RawResponse(code = 200, headers = Map.empty, body = "OK", delay = None),
      callback = None,
      labels = labels,
    )
  }

}
