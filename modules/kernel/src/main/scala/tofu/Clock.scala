package tofu

import tofu.internal.EffectComp
import tofu.internal.carriers._

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

trait Clock[F[_]] {

  /** Returns the current time, as a Unix timestamp (number of time units
    * since the Unix epoch), suspended in `F[_]`.
    *
    * This is the pure equivalent of Java's `System.currentTimeMillis`,
    * or of `CLOCK_REALTIME` from Linux's `clock_gettime()`.
    */
  def realTime(unit: TimeUnit): F[Long]

  /** Returns a monotonic clock measurement, if supported by the
    * underlying platform.
    *
    * This is the pure equivalent of Java's `System.nanoTime`,
    * or of `CLOCK_MONOTONIC` from Linux's `clock_gettime()`.
    */
  def nanos: F[Long]
}

object Clock extends EffectComp[Clock] with ClockInterop

trait Sleep[F[_]] {

  /** Pauses execution for desired duration
    */
  def sleep(duration: FiniteDuration): F[Unit]
}

object Sleep extends EffectComp[Sleep] with SleepInterop

trait Timeout[F[_]] {
  def timeoutTo[A](fa: F[A], after: FiniteDuration, fallback: F[A]): F[A]
}

object Timeout extends TimeoutInterop with EffectComp[Timeout]

trait ClockInterop extends ClockInterop1 {
  implicit def ce3Interop[F[_]](implicit clock: ClockCE3Carrier[F]): Clock[F] = clock
}

trait ClockInterop1 {
  implicit def ce2Interop[F[_]](implicit clock: ClockCE2Carrier[F]): Clock[F] = clock
}

trait SleepInterop extends SleepInterop1 {
  implicit def ce3Interop[F[_]](implicit sleep: SleepCE3Carrier[F]): Sleep[F] = sleep
}

trait SleepInterop1 {
  implicit def ce2Interop[F[_]](implicit sleep: SleepCE2Carrier[F]): Sleep[F] = sleep
}

trait TimeoutInterop extends TimeoutInterop1 {
  implicit def ce3Interop[F[_]](implicit timeout: TimeoutCE3Carrier[F]): Timeout[F] = timeout
}

trait TimeoutInterop1 {
  implicit def ce2Interop[F[_]](implicit timeout: TimeoutCE2Carrier[F]): Timeout[F] = timeout
}
