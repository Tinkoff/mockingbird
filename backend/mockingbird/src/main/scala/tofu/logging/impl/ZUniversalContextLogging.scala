package tofu.logging.impl

import org.slf4j.LoggerFactory
import org.slf4j.Marker
import tofu.logging.Loggable
import tofu.logging.LoggedValue
import tofu.logging.Logging

class ZUniversalContextLogging[R, C: Loggable](name: String, ctxLog: URIO[R, C]) extends Logging[URIO[R, *]] {
  def write(level: Logging.Level, message: String, values: LoggedValue*): URIO[R, Unit] =
    ctxLog.flatMap { ctx =>
      ZIO.succeed {
        val logger = LoggerFactory.getLogger(name)
        if (UniversalLogging.enabled(level, logger))
          UniversalLogging.writeMarker(level, logger, ContextMarker(ctx), message, values)
      }
    }

  override def writeMarker(level: Logging.Level, message: String, marker: Marker, values: LoggedValue*): URIO[R, Unit] =
    ctxLog.flatMap { ctx =>
      ZIO.succeed {
        val logger = LoggerFactory.getLogger(name)
        if (UniversalLogging.enabled(level, logger))
          UniversalLogging.writeMarker(level, logger, ContextMarker(ctx, List(marker)), message, values)
      }
    }

  override def writeCause(
      level: Logging.Level,
      message: String,
      cause: Throwable,
      values: LoggedValue*
  ): URIO[R, Unit] =
    ctxLog.flatMap { ctx =>
      ZIO.succeed {
        val logger = LoggerFactory.getLogger(name)
        if (UniversalLogging.enabled(level, logger))
          UniversalLogging.writeMarkerCause(level, logger, ContextMarker(ctx), cause, message, values)
      }
    }
}
