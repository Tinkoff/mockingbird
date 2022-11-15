package ru.tinkoff.tcb.mockingbird.grpc

import com.google.protobuf.InvalidProtocolBufferException
import io.circe.Json
import io.circe.ParsingFailure
import mouse.option.*
import zio.interop.catz.core.*

import ru.tinkoff.tcb.criteria.*
import ru.tinkoff.tcb.criteria.Typed.*
import ru.tinkoff.tcb.logging.MDCLogging
import ru.tinkoff.tcb.mockingbird.api.WLD
import ru.tinkoff.tcb.mockingbird.dal.GrpcStubDAO
import ru.tinkoff.tcb.mockingbird.dal.PersistentStateDAO
import ru.tinkoff.tcb.mockingbird.error.StubSearchError
import ru.tinkoff.tcb.mockingbird.grpc.GrpcExractor.FromGrpcProtoDefinition
import ru.tinkoff.tcb.mockingbird.misc.Renderable.ops.*
import ru.tinkoff.tcb.mockingbird.model.GrpcStub
import ru.tinkoff.tcb.mockingbird.model.PersistentState
import ru.tinkoff.tcb.mockingbird.model.Scope
import ru.tinkoff.tcb.predicatedsl.Keyword
import ru.tinkoff.tcb.utils.circe.optics.JsonOptic
import ru.tinkoff.tcb.utils.id.SID

trait GrpcStubResolver {
  def findStubAndState(service: String, request: Array[Byte])(
      scope: Scope
  ): RIO[WLD, Option[(GrpcStub, Json, Option[PersistentState])]]
}

class GrpcStubResolverImpl(stubDAO: GrpcStubDAO[Task], stateDAO: PersistentStateDAO[Task]) extends GrpcStubResolver {

  private type StateSpec = Map[JsonOptic, Map[Keyword.Json, Json]]

  private val log = MDCLogging.`for`[WLD](this)

  def findStubAndState(
      methodName: String,
      request: Array[Byte]
  )(scope: Scope): RIO[WLD, Option[(GrpcStub, Json, Option[PersistentState])]] = for {
    stubs <- stubDAO
      .findChunk(
        prop[GrpcStub](_.methodName) === methodName &&
          prop[GrpcStub](_.scope) === scope &&
          prop[GrpcStub](_.times) > Option(0),
        0,
        Integer.MAX_VALUE
      )
    parsed <- ZIO.foreachPar(stubs)(parseJson(_, request)).map(_.flatten)
    pairs = parsed
      .flatMap { case (json, id) =>
        stubs
          .find(_.id == id)
          .map(json -> _)
      }
      .filter { case (json, stub) =>
        stub.requestPredicates(json)
      }
    candidates <- pairs
      .traverse { case (json, stub) =>
        stub.state
          .map(_.fill(json))
          .cata(
            spec => findStates(stub.id, spec).map(stub -> _),
            ZIO.succeed(stub -> Vector.empty[PersistentState])
          )
      }
    _ <- ZIO.when(candidates.exists(_._2.size > 1))(
      log.error("Для одной или нескольких заглушек найдено более одного подходящего состояния") *>
        ZIO.fail(StubSearchError("Для одной или нескольких заглушек найдено более одного подходящего состояния"))
    )
    _ <- ZIO.when(candidates.count(_._2.nonEmpty) > 1)(
      log.error("Для более чем одной заглушки нашлось подходящее состояние") *>
        ZIO.fail(StubSearchError("Для более чем одной заглушки нашлось подходящее состояние"))
    )
    _ <- ZIO.when(candidates.size > 1 && candidates.forall(c => c._1.state.isDefined && c._2.isEmpty))(
      log.error("Ни для одной заглушки не найдено подходящего состояния") *>
        ZIO.fail(StubSearchError("Ни для одной заглушки не найдено подходящего состояния"))
    )
    _ <- ZIO.when(candidates.size > 1 && candidates.forall(_._1.state.isEmpty))(
      log.error("Найдено более одной не требующей состояния заглушки") *>
        ZIO.fail(StubSearchError("Найдено более одной не требующей состояния заглушки"))
    )
    res = candidates.find(_._2.size == 1) orElse candidates.find(_._1.state.isEmpty)
  } yield res.map { case (stub, states) =>
    (stub, pairs.find(_._2.id == stub.id).map(_._1).get, states.headOption)
  }

  private def parseJson(stub: GrpcStub, bytes: Array[Byte]): UIO[Option[(Json, SID[GrpcStub])]] =
    ZIO
      .blocking(
        stub.requestSchema.convertMessageToJson(bytes, stub.requestClass).map(json => (json, stub.id).some)
      )
      .catchSome { case _: InvalidProtocolBufferException | ParsingFailure(_, _) =>
        ZIO.none
      }
      .orDie

  private def findStates(id: SID[GrpcStub], spec: StateSpec): RIO[WLD, Vector[PersistentState]] =
    for {
      _      <- log.info("Поиск state для {} по условию {}", id, spec.renderJson.noSpaces)
      states <- stateDAO.findBySpec(spec)
      _ <-
        if (states.nonEmpty) log.info("Найдены состояния для {}: {}", id, states.map(_.id))
        else log.info("Не найдено подходящих состояний для {}", id)
    } yield states
}

object GrpcStubResolverImpl {
  val live: URLayer[GrpcStubDAO[Task] & PersistentStateDAO[Task], GrpcStubResolver] =
    ZLayer {
      for {
        gsd <- ZIO.service[GrpcStubDAO[Task]]
        psd <- ZIO.service[PersistentStateDAO[Task]]
      } yield new GrpcStubResolverImpl(gsd, psd)
    }
}
