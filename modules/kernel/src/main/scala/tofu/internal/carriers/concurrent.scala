package tofu.internal.carriers

import tofu.internal.Interop
import tofu.concurrent.MakeAtom
import tofu.concurrent.MakeQVar

trait MkAtomCE2Carrier[I[_], F[_]] extends MakeAtom[I, F]

object MkAtomCE2Carrier {
  final implicit def interopCE2Carrier[I[_], F[_]]: MkAtomCE2Carrier[I, F] =
    macro Interop.delegate2[MkAtomCE2Carrier[I, F], I, F, { val `tofu.interop.CE2Kernel.atomBySync`: Unit }]
}

trait MkAtomCE3Carrier[I[_], F[_]] extends MakeAtom[I, F]

object MkAtomCE3Carrier {
  final implicit def interopCE3Carrier[I[_], F[_]]: MkAtomCE3Carrier[I, F] =
    macro Interop.delegate2[MkAtomCE2Carrier[I, F], I, F, { val `tofu.interop.CE3Kernel.atomBySync`: Unit }]
}

trait MkQVarCE2Carrier[I[_], F[_]] extends MakeQVar[I, F]

object MkQVarCE2Carrier {
  final implicit def interopCE2Carrier[I[_], F[_]]: MkQVarCE2Carrier[I, F] =
    macro Interop.delegate2[MkQVarCE2Carrier[I, F], I, F, { val `tofu.interop.CE2Kernel.qvarByConcurrent`: Unit }]
}

trait MkQVarCE3Carrier[I[_], F[_]] extends MakeQVar[I, F]

object MkQVarCE3Carrier {
  final implicit def interopCE3Carrier[I[_], F[_]]: MkQVarCE3Carrier[I, F] =
    macro Interop.delegate2[MkQVarCE3Carrier[I, F], I, F, { val `tofu.interop.CE3Kernel.qvarByConcurrent`: Unit }]
}
