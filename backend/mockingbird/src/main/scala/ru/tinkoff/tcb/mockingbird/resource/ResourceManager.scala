package ru.tinkoff.tcb.mockingbird.resource

import scala.annotation.nowarn
import scala.util.control.NonFatal

import mouse.option.*
import sttp.client3.*
import sttp.model.Method
import zio.managed.*

import ru.tinkoff.tcb.logging.MDCLogging
import ru.tinkoff.tcb.mockingbird.api.WLD
import ru.tinkoff.tcb.mockingbird.dal.DestinationConfigurationDAO
import ru.tinkoff.tcb.mockingbird.dal.SourceConfigurationDAO
import ru.tinkoff.tcb.mockingbird.error.CompoundError
import ru.tinkoff.tcb.mockingbird.error.ResourceManagementError
import ru.tinkoff.tcb.mockingbird.model.ResourceRequest

final class ResourceManager(
    private val httpBackend: SttpBackend[Task, ?],
    sourceDAO: SourceConfigurationDAO[Task],
    destinationDAO: DestinationConfigurationDAO[Task]
) {
  private val log = MDCLogging.`for`[WLD](this)

  @nowarn("cat=other-match-analysis")
  def startup(): URIO[WLD, Unit] =
    (for {
      sources      <- sourceDAO.getAll
      destinations <- destinationDAO.getAll
      inits = sources.flatMap(_.init.map(_.toVector).getOrElse(Vector.empty)) ++ destinations.flatMap(
        _.init.map(_.toVector).getOrElse(Vector.empty)
      )
      _ <- ZIO.validateDiscard(inits)(execute).mapError(CompoundError)
    } yield ()).catchAll {
      case CompoundError(errs) if errs.forall(recover.isDefinedAt) =>
        ZIO.foreachDiscard(errs)(recover)
      case CompoundError(errs) =>
        val recoverable = errs.filter(recover.isDefinedAt)
        val fatal       = errs.find(!recover.isDefinedAt(_))
        ZIO.foreachDiscard(recoverable)(recover) *> log.errorCause("Fatal error", fatal.get) *> ZIO.die(fatal.get)
      case thr if recover.isDefinedAt(thr) =>
        recover(thr)
      case thr =>
        log.errorCause("Fatal error", thr) *> ZIO.die(thr)
    }

  @nowarn("cat=other-match-analysis")
  def shutdown(): URIO[WLD, Unit] =
    (for {
      sources      <- sourceDAO.getAll
      destinations <- destinationDAO.getAll
      shutdowns = sources.flatMap(_.shutdown.map(_.toVector).getOrElse(Vector.empty)) ++ destinations.flatMap(
        _.shutdown.map(_.toVector).getOrElse(Vector.empty)
      )
      _ <- ZIO.validateDiscard(shutdowns)(execute).mapError(CompoundError)
    } yield ()).catchAll {
      case CompoundError(errs) if errs.forall(recover.isDefinedAt) =>
        ZIO.foreachDiscard(errs)(recover)
      case CompoundError(errs) =>
        val recoverable = errs.filter(recover.isDefinedAt)
        val fatal       = errs.find(!recover.isDefinedAt(_))
        ZIO.foreachDiscard(recoverable)(recover) *> log.errorCause("Fatal error", fatal.get) *> ZIO.die(fatal.get)
      case thr if recover.isDefinedAt(thr) =>
        recover(thr)
      case thr =>
        log.errorCause("Fatal error", thr) *> ZIO.die(thr)
    }

  def execute(req: ResourceRequest): Task[String] =
    (basicRequest
      .headers(req.headers.view.mapValues(_.asString).toMap))
      .pipe(r => req.body.cata(b => r.body(b.asString), r))
      .method(Method(req.method.entryName), uri"${req.url.asString}")
      .response(asString("utf-8"))
      .readTimeout(30.seconds.asScala)
      .send(httpBackend)
      .map(_.body)
      .right
      .mapError {
        case Right(err) => err
        case Left(err) =>
          ResourceManagementError(s"Запрос на ${req.url.asString} завершился ошибкой ($err)")
      }

  private val recover: PartialFunction[Throwable, URIO[WLD, Unit]] = {
    case ResourceManagementError(msg) =>
      log.warn(msg)
    case NonFatal(exc) =>
      log.warnCause("Unexpected error", exc)
  }
}

object ResourceManager {
  val live = ZLayer {
    for {
      sttpClient <- ZIO.service[SttpBackend[Task, Any]]
      srcd       <- ZIO.service[SourceConfigurationDAO[Task]]
      destd      <- ZIO.service[DestinationConfigurationDAO[Task]]
    } yield new ResourceManager(sttpClient, srcd, destd)
  }

  val managed: ZManaged[WLD & ResourceManager, Throwable, ResourceManager] =
    (for {
      rm <- ZIO.service[ResourceManager]
      _  <- rm.startup()
    } yield rm).toManagedWith(_.shutdown())

}
