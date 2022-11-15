package ru.tinkoff.tcb.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tofu.logging.Loggable
import tofu.logging.LoggedValue
import tofu.logging.Logging
import tofu.logging.Logging.Debug
import tofu.logging.Logging.Error
import tofu.logging.Logging.Info
import tofu.logging.Logging.Trace
import tofu.logging.Logging.Warn

import ru.tinkoff.tcb.mockingbird.api.Tracing

class MDCLogging[R <: Tracing](logger: Logger) extends Logging[URIO[R, *]] {
  override def write(level: Logging.Level, message: String, values: LoggedValue*): URIO[Tracing, Unit] =
    level match {
      case Trace =>
        ZIO
          .when(logger.isTraceEnabled) {
            for {
              tracing <- ZIO.service[Tracing]
              ctx     <- tracing.lc.get
              mdc = Loggable[Mdc].loggedValue(ctx.mdc())
              _ <- ZIO.succeed(logger.trace(message, values :+ mdc: _*))
            } yield ()
          }
          .unit
      case Debug =>
        ZIO
          .when(logger.isDebugEnabled) {
            for {
              tracing <- ZIO.service[Tracing]
              ctx     <- tracing.lc.get
              mdc = Loggable[Mdc].loggedValue(ctx.mdc())
              _ <- ZIO.succeed(logger.debug(message, values :+ mdc: _*))
            } yield ()
          }
          .unit
      case Info =>
        ZIO
          .when(logger.isInfoEnabled) {
            for {
              tracing <- ZIO.service[Tracing]
              ctx     <- tracing.lc.get
              mdc = Loggable[Mdc].loggedValue(ctx.mdc())
              _ <- ZIO.succeed(logger.info(message, values :+ mdc: _*))
            } yield ()
          }
          .unit
      case Warn =>
        ZIO
          .when(logger.isWarnEnabled) {
            for {
              tracing <- ZIO.service[Tracing]
              ctx     <- tracing.lc.get
              mdc = Loggable[Mdc].loggedValue(ctx.mdc())
              _ <- ZIO.succeed(logger.warn(message, values :+ mdc: _*))
            } yield ()
          }
          .unit
      case Error =>
        ZIO
          .when(logger.isErrorEnabled) {
            for {
              tracing <- ZIO.service[Tracing]
              ctx     <- tracing.lc.get
              mdc = Loggable[Mdc].loggedValue(ctx.mdc())
              _ <- ZIO.succeed(logger.error(message, values :+ mdc: _*))
            } yield ()
          }
          .unit
    }
}

object MDCLogging {
  def `for`[R <: Tracing](t: AnyRef): MDCLogging[R]          = forClass(t.getClass)
  def forClass[R <: Tracing](klass: Class[?]): MDCLogging[R] = new MDCLogging(LoggerFactory.getLogger(klass))
}
