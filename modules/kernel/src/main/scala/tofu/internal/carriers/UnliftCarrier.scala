package tofu.internal.carriers

import tofu.internal.Interop
import tofu.lift.Unlift

// This is purely workaround for scala 2.12
// Which denies to unfold a macro (and recieve a type error)
// before checking an implicit for eligibility
trait UnliftCarrier[F[_], G[_]] extends Unlift[F, G]

object UnliftCarrier {
  final implicit def unliftIOEffect[F[_], G[_]]: UnliftCarrier[F, G] =
    macro Interop.delegate[UnliftCarrier[F, G], G, { val `tofu.interop.CE2Kernel.unliftEffect`: Unit }]
}
