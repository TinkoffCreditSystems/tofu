package tofu
package syntax

import java.time.{Clock => _, _}
import java.util.concurrent.TimeUnit

import cats.{Apply, Functor}
import tofu.common.{TimeData, TimeZone}
import tofu.syntax.monadic._
import scala.concurrent.duration.FiniteDuration

object time {
  def sleep[F[_]](duration: FiniteDuration)(implicit sleep: Sleep[F]): F[Unit] = sleep.sleep(duration)

  object zone {

    /** current System time zone */
    def system[F[_]](implicit tz: TimeZone[F]): F[ZoneId] = tz.system

    /** list of available time zones */
    def available[F[_]](implicit tz: TimeZone[F]): F[Set[String]] = tz.available

    /** get ZoneId by full string ID,
      * checking for availability, probably generating an error
      * see documentation to `java.time.ZoneId.of`
      */
    def of[F[_]](zoneId: String)(implicit tz: TimeZone[F]): F[ZoneId] = tz.of(zoneId)

    /** Obtains an instance of {@code ZoneId} wrapping an offset.
      * <p>
      * If the prefix is "GMT", "UTC", or "UT" a {@code ZoneId}
      * with the prefix and the non-zero offset is returned.
      * If the prefix is empty {@code ""} the {@code ZoneOffset} is returned.
      */
    def ofOffset[F[_]](prefix: String, offset: ZoneOffset)(implicit tz: TimeZone[F]): F[ZoneId] =
      tz.ofOffset(prefix, offset)
  }

  object now {
    def millis[F[_]](implicit clock: Clock[F]): F[Long]    = clock.realTime(TimeUnit.MILLISECONDS)
    def monoNanos[F[_]](implicit clock: Clock[F]): F[Long] = clock.nanos

    def instant[F[_]: Clock: Functor]: F[Instant] = millis[F].map(Instant.ofEpochMilli)

    def apply[F[_]: Apply: TimeZone: Clock, A](implicit td: TimeData[A]): F[A] =
      instant[F].map2(zone.system)(td.fromInstant)
  }

  implicit class ZoneOps(private val zoneId: ZoneId) extends AnyVal {
    def current[F[_]: Functor: Clock, A](implicit td: TimeData[A]): F[A] = now.instant[F].map(td.fromInstant(_, zoneId))
  }

}
