package ru.tinkoff.tcb.mockingbird.stream

import eu.timepit.fs2cron.awakeEveryCron
import fs2.Stream
import tofu.logging.Logging
import tofu.logging.impl.ZUniversalLogging
import zio.interop.catz.*
import zio.interop.catz.implicits.*

import ru.tinkoff.tcb.criteria.*
import ru.tinkoff.tcb.criteria.Typed.*
import ru.tinkoff.tcb.mockingbird.dal.HttpStubDAO
import ru.tinkoff.tcb.mockingbird.dal.ScenarioDAO
import ru.tinkoff.tcb.mockingbird.model.HttpStub
import ru.tinkoff.tcb.mockingbird.model.Scenario
import ru.tinkoff.tcb.mockingbird.model.Scope

final class EphemeralCleaner(stubDAO: HttpStubDAO[Task], scenarioDAO: ScenarioDAO[Task]) {
  private val log: Logging[UIO] = new ZUniversalLogging(this.getClass.getName)

  private val trigger = awakeEveryCron[Task](midnight)

  private val secondsInDay = 60 * 60 * 24

  private val cleanup = Stream.eval[Task, Long] {
    for {
      current <- ZIO.clock.flatMap(_.instant)
      threshold = current.minusSeconds(secondsInDay)
      deleted <- stubDAO.delete(
        prop[HttpStub](_.scope).in[Scope](Scope.Ephemeral, Scope.Countdown) && prop[HttpStub](_.created) < threshold
      )
      _ <- log.info("Purging expired stubs: {} deleted", deleted)
      deleted2 <- scenarioDAO.delete(
        prop[Scenario](_.scope).in[Scope](Scope.Ephemeral, Scope.Countdown) && prop[Scenario](_.created) < threshold
      )
      _ <- log.info("Purging expired scenarios: {} deleted", deleted2)
      deleted3 <- stubDAO.delete(
        prop[HttpStub](_.scope) === Scope.Countdown.asInstanceOf[Scope] && prop[HttpStub](_.times) <= Option(0)
      )
      _ <- log.info("Purging countdown stubs: {} deleted", deleted3)
      deleted4 <- scenarioDAO.delete(
        prop[Scenario](_.scope) === Scope.Countdown.asInstanceOf[Scope] && prop[Scenario](_.times) <= Option(0)
      )
      _ <- log.info("Purging countdown scenarios: {} deleted", deleted4)
    } yield deleted
  }

  private val stream = trigger >> cleanup

  def run: Task[Unit] = stream.compile.drain
}

object EphemeralCleaner {
  val live = ZLayer {
    for {
      hsd <- ZIO.service[HttpStubDAO[Task]]
      sd  <- ZIO.service[ScenarioDAO[Task]]
    } yield new EphemeralCleaner(hsd, sd)
  }
}
