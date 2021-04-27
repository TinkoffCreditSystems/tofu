package tofu.syntax.streams

import cats.syntax.semigroupk._
import cats.{Defer, SemigroupK}

object combineK {

  implicit class CombineKOps[F[_], A](private val fa: F[A]) extends AnyVal {
    def repeat(implicit F: SemigroupK[F], D: Defer[F]): F[A] = fa <+> D.defer(repeat)
  }
}
