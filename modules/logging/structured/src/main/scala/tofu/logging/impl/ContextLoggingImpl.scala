package tofu
package logging
package impl

import cats.Applicative
import cats.syntax.applicative._
import org.slf4j.{Logger, Marker}

class ContextLoggingImpl[F[_]: Applicative, C: Loggable, Service](context: F WithContext C, logger: Logger)
    extends LoggingImpl[F](logger) {

  override def trace(message: String, values: LoggedValue*) =
    context.ask(ctx => logger.trace(ContextMarker(ctx), message, values: _*)).whenA(traceEnabled)
  override def debug(message: String, values: LoggedValue*) =
    context.ask(ctx => logger.debug(ContextMarker(ctx), message, values: _*)).whenA(debugEnabled)
  override def info(message: String, values: LoggedValue*)  =
    context.ask(ctx => logger.info(ContextMarker(ctx), message, values: _*)).whenA(infoEnabled)
  override def warn(message: String, values: LoggedValue*)  =
    context.ask(ctx => logger.warn(ContextMarker(ctx), message, values: _*)).whenA(warnEnabled)
  override def error(message: String, values: LoggedValue*) =
    context.ask(ctx => logger.error(ContextMarker(ctx), message, values: _*)).whenA(errorEnabled)

  override def traceWithMarker(message: String, marker: Marker, values: LoggedValue*): F[Unit] =
    context.ask(ctx => logger.trace(ContextMarker(ctx).addMarker(marker), message, values: _*)).whenA(traceEnabled)
  override def debugWithMarker(message: String, marker: Marker, values: LoggedValue*): F[Unit] =
    context.ask(ctx => logger.debug(ContextMarker(ctx).addMarker(marker), message, values: _*)).whenA(debugEnabled)
  override def infoWithMarker(message: String, marker: Marker, values: LoggedValue*): F[Unit]  =
    context.ask(ctx => logger.info(ContextMarker(ctx).addMarker(marker), message, values: _*)).whenA(infoEnabled)
  override def warnWithMarker(message: String, marker: Marker, values: LoggedValue*): F[Unit]  =
    context.ask(ctx => logger.warn(ContextMarker(ctx).addMarker(marker), message, values: _*)).whenA(warnEnabled)
  override def errorWithMarker(message: String, marker: Marker, values: LoggedValue*): F[Unit] =
    context.ask(ctx => logger.error(ContextMarker(ctx).addMarker(marker), message, values: _*)).whenA(errorEnabled)

  override def traceCause(message: String, cause: Throwable, values: LoggedValue*): F[Unit] =
    context.ask(ctx => logger.trace(ContextMarker(ctx), message, values :+ cause: _*)).whenA(traceEnabled)
  override def debugCause(message: String, cause: Throwable, values: LoggedValue*): F[Unit] =
    context.ask(ctx => logger.debug(ContextMarker(ctx), message, values :+ cause: _*)).whenA(debugEnabled)
  override def infoCause(message: String, cause: Throwable, values: LoggedValue*): F[Unit]  =
    context.ask(ctx => logger.info(ContextMarker(ctx), message, values :+ cause: _*)).whenA(infoEnabled)
  override def warnCause(message: String, cause: Throwable, values: LoggedValue*): F[Unit]  =
    context.ask(ctx => logger.error(ContextMarker(ctx), message, values :+ cause: _*)).whenA(warnEnabled)
  override def errorCause(message: String, cause: Throwable, values: LoggedValue*): F[Unit] =
    context.ask(ctx => logger.error(ContextMarker(ctx), message, values :+ cause: _*)).whenA(errorEnabled)
}
