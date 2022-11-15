package ru.tinkoff.tcb.mockingbird.api

import java.util.UUID

import ru.tinkoff.tcb.logging.LogContext

final case class Tracing(lc: FiberRef[LogContext])

object Tracing {
  val fresh: URIO[Scope, Tracing] = FiberRef.make[LogContext](LogContext.empty).map(Tracing(_))
  val live: ULayer[Tracing]       = ZLayer.scoped(fresh)

  val init: RIO[Tracing, Unit] = for {
    tracing <- ZIO.service[Tracing]
    cid = UUID.randomUUID()
    _ <- tracing.lc.update(_.setCorrelationId(cid.toString))
  } yield ()

  val update: (LogContext => LogContext) => RIO[Tracing, Unit] = f =>
    for {
      tracing <- ZIO.service[Tracing]
      _       <- tracing.lc.update(f)
    } yield ()
}
