package ru.tinkoff.tcb.mockingbird.stream

import scala.annotation.nowarn
import scala.util.control.NonFatal

import fs2.Stream
import zio.interop.catz.*
import zio.interop.catz.implicits.*

import ru.tinkoff.tcb.logging.MDCLogging
import ru.tinkoff.tcb.mockingbird.api.WLD
import ru.tinkoff.tcb.mockingbird.config.EventConfig
import ru.tinkoff.tcb.mockingbird.dal.DestinationConfigurationDAO
import ru.tinkoff.tcb.mockingbird.dal.SourceConfigurationDAO
import ru.tinkoff.tcb.mockingbird.model.DestinationConfiguration
import ru.tinkoff.tcb.mockingbird.model.SourceConfiguration

final class SDFetcher(
    eventConfig: EventConfig,
    sourceCache: Ref[Vector[SourceConfiguration]],
    sourceDAO: SourceConfigurationDAO[Task],
    destionationCache: Ref[Vector[DestinationConfiguration]],
    destinationDAO: DestinationConfigurationDAO[Task]
) {
  private val log = MDCLogging.`for`[WLD](this)

  @nowarn("cat=other-match-analysis")
  private def reloadSrc: Stream[RIO[WLD, *], Unit] =
    Stream
      .awakeEvery[RIO[WLD, *]](eventConfig.reloadInterval)
      .evalMap(_ => sourceDAO.getAll)
      .evalTap(sourceCache.set)
      .evalMap(srcs => log.info("Получены источники: {}", srcs.map(_.name)))
      .handleErrorWith { case NonFatal(t) =>
        Stream.eval(log.errorCause("Ошибка при загрузке источников", t)) ++
          Stream.sleep[RIO[WLD, *]](eventConfig.reloadInterval) ++ reloadSrc
      }

  @nowarn("cat=other-match-analysis")
  private def reloadDest: Stream[RIO[WLD, *], Unit] =
    Stream
      .awakeEvery[RIO[WLD, *]](eventConfig.reloadInterval)
      .evalMap(_ => destinationDAO.getAll)
      .evalTap(destionationCache.set)
      .evalMap(dsts => log.info("Получены приёмники: {}", dsts.map(_.name)))
      .handleErrorWith { case NonFatal(t) =>
        Stream.eval(log.errorCause("Ошибка при загрузке приёмников", t)) ++
          Stream.sleep[RIO[WLD, *]](eventConfig.reloadInterval) ++ reloadDest
      }

  def getSources: UIO[Vector[SourceConfiguration]] = sourceCache.get

  def getDestinations: UIO[Vector[DestinationConfiguration]] = destionationCache.get

  def run: RIO[WLD, Unit] =
    reloadSrc.compile.drain <&> reloadDest.compile.drain
}

object SDFetcher {
  val live = ZLayer {
    for {
      config    <- ZIO.service[EventConfig]
      scache    <- Ref.make(Vector.empty[SourceConfiguration])
      sourceDAO <- ZIO.service[SourceConfigurationDAO[Task]]
      dcache    <- Ref.make(Vector.empty[DestinationConfiguration])
      destDAO   <- ZIO.service[DestinationConfigurationDAO[Task]]
    } yield new SDFetcher(config, scache, sourceDAO, dcache, destDAO)
  }
}
