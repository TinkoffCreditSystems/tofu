package tofu.logging
package impl

import org.slf4j.{Logger, Marker}
import tofu.syntax.monadic._
import cats.Monad
import tofu.Delay

class SyncLogging[F[_]: Monad](logger: Logger)(implicit F: Delay[F]) extends LoggingImpl[F](logger) {
  override def trace(message: String, values: LoggedValue*): F[Unit] =
    F.delay(logger.trace(message, values: _*)).whenA(traceEnabled)
  override def debug(message: String, values: LoggedValue*): F[Unit] =
    F.delay(logger.debug(message, values: _*)).whenA(debugEnabled)
  override def info(message: String, values: LoggedValue*): F[Unit]  =
    F.delay(logger.info(message, values: _*)).whenA(infoEnabled)
  override def warn(message: String, values: LoggedValue*): F[Unit]  =
    F.delay(logger.warn(message, values: _*)).whenA(warnEnabled)
  override def error(message: String, values: LoggedValue*): F[Unit] =
    F.delay(logger.error(message, values: _*)).whenA(errorEnabled)

  override def traceWithMarker(message: String, marker: Marker, values: LoggedValue*): F[Unit] =
    F.delay(logger.trace(marker, message, values: _*)).whenA(traceEnabled)
  override def debugWithMarker(message: String, marker: Marker, values: LoggedValue*): F[Unit] =
    F.delay(logger.debug(marker, message, values: _*)).whenA(debugEnabled)
  override def infoWithMarker(message: String, marker: Marker, values: LoggedValue*): F[Unit]  =
    F.delay(logger.info(marker, message, values: _*)).whenA(infoEnabled)
  override def warnWithMarker(message: String, marker: Marker, values: LoggedValue*): F[Unit]  =
    F.delay(logger.warn(marker, message, values: _*)).whenA(warnEnabled)
  override def errorWithMarker(message: String, marker: Marker, values: LoggedValue*): F[Unit] =
    F.delay(logger.error(marker, message, values: _*)).whenA(errorEnabled)

  override def errorCause(message: String, cause: Throwable, values: LoggedValue*): F[Unit] =
    F.delay(logger.error(message, values :+ cause: _*)).whenA(errorEnabled)
  override def warnCause(message: String, cause: Throwable, values: LoggedValue*): F[Unit]  =
    F.delay(logger.warn(message, values :+ cause: _*)).whenA(warnEnabled)
  override def infoCause(message: String, cause: Throwable, values: LoggedValue*): F[Unit]  =
    F.delay(logger.info(message, values :+ cause: _*)).whenA(infoEnabled)
  override def debugCause(message: String, cause: Throwable, values: LoggedValue*): F[Unit] =
    F.delay(logger.debug(message, values :+ cause: _*)).whenA(debugEnabled)
  override def traceCause(message: String, cause: Throwable, values: LoggedValue*): F[Unit] =
    F.delay(logger.trace(message, values :+ cause: _*)).whenA(traceEnabled)
}
