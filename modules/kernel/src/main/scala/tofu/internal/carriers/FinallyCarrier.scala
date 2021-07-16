package tofu.internal.carriers

import tofu.Finally
import tofu.internal.WBInterop

trait FinallyCarrier[F[_], E] {
  type Exit[_]
  def content: Finally[F, Exit]
}

object FinallyCarrier {
  type Aux[F[_], E, Ex[_]] = FinallyCarrier[F, E] { type Exit[a] = Ex[a] }

  trait Impl[F[_], E, Ex[_]] extends FinallyCarrier[F, E] with Finally[F, Ex] {
    type Exit[a] = Ex[a]
    def content = this
  }

  final implicit def fromBracket[F[_], E, Exit[_]]: Aux[F, E, Exit] =
    macro WBInterop.delegate1[F, E, { val `tofu.interop.CE2Kernel.finallyFromBracket`: Unit }]
}
