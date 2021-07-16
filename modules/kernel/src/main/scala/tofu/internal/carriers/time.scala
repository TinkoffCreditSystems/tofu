package tofu.internal
package carriers

import tofu.{Clock, Sleep}
import tofu.Timeout

trait ClockCE2Carrier[F[_]] extends Clock[F]

object ClockCE2Carrier {
  final implicit def interop[F[_]]: ClockCE2Carrier[F] =
    macro Interop.delegate[ClockCE2Carrier[F], F, { val `tofu.interop.CE2Kernel.clock`: Unit }]
}

trait ClockCE3Carrier[F[_]] extends Clock[F]

object ClockCE3Carrier {
  final implicit def interop[F[_]]: ClockCE3Carrier[F] =
    macro Interop.delegate[ClockCE3Carrier[F], F, { val `tofu.interop.CE3Kernel.clock`: Unit }]
}

trait SleepCE2Carrier[F[_]] extends Sleep[F]

object SleepCE2Carrier {
  final implicit def interop[F[_]]: SleepCE2Carrier[F] =
    macro Interop.delegate[SleepCE2Carrier[F], F, { val `tofu.interop.CE2Kernel.sleep`: Unit }]
}

trait SleepCE3Carrier[F[_]] extends Sleep[F]

object SleepCE3Carrier {
  final implicit def interop[F[_]]: SleepCE3Carrier[F] =
    macro Interop.delegate[SleepCE3Carrier[F], F, { val `tofu.interop.CE3Kernel.sleep`: Unit }]
}

trait TimeoutCE2Carrier[F[_]] extends Timeout[F]

object TimeoutCE2Carrier {
  final implicit def interop[F[_]]: TimeoutCE2Carrier[F] =
    macro Interop.delegate[TimeoutCE2Carrier[F], F, { val `tofu.interop.CE2Kernel.timeout`: Unit }]
}

trait TimeoutCE3Carrier[F[_]] extends Timeout[F]

object TimeoutCE3Carrier {
  final implicit def interop[F[_]]: TimeoutCE3Carrier[F] =
    macro Interop.delegate[TimeoutCE3Carrier[F], F, { val `tofu.interop.CE3Kernel.timeout`: Unit }]
}
