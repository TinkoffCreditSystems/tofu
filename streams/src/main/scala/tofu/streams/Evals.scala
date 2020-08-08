package tofu.streams

import cats.syntax.flatMap._
import cats.{Applicative, Foldable, Monad, MonoidK}

trait Emits[F[_]] {

  val monoidK: MonoidK[F]
  val applicative: Applicative[F]

  final def emits[C[_], A](as: C[A])(implicit C: Foldable[C]): F[A] =
    C.foldLeft(as, monoidK.empty[A])((acc, a) => monoidK.combineK(acc, applicative.pure(a)))
}

object Emits {

  implicit def instance[F[_]: MonoidK: Applicative]: Emits[F] =
    new Emits[F] {
      override val monoidK: MonoidK[F]         = implicitly
      override val applicative: Applicative[F] = implicitly
    }
}

trait Evals[F[_], G[_]] extends Emits[F] {

  implicit val monad: Monad[F]

  val applicative: Applicative[F] = monad

  def eval[A](ga: G[A]): F[A]

  final def evals[C[_]: Foldable, A](gsa: G[C[A]]): F[A] =
    eval(gsa) >>= (emits(_))

  final def evalMap[A, B](fa: F[A])(f: A => G[B]): F[B] =
    fa >>= (a => eval(f(a)))
}