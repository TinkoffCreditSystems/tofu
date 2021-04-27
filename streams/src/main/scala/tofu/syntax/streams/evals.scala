package tofu.syntax.streams

import cats.syntax.functor._
import cats.{Foldable, Functor}
import tofu.lift.Lift
import tofu.streams.Evals

object evals {

  def eval[F[_]]: EvalPA[F] = new EvalPA[F](true)

  private[syntax] final class EvalPA[F[_]](private val __ : Boolean) extends AnyVal {
    def apply[G[_], A](ga: G[A])(implicit ev: Lift[G, F]): F[A] = ev.lift(ga)
  }

  def evals[F[_]]: EvalsPA[F] = new EvalsPA[F](true)

  private[syntax] final class EvalsPA[F[_]](private val __ : Boolean) extends AnyVal {
    def apply[G[_], C[_]: Foldable, A](gca: G[C[A]])(implicit ev: Evals[F, G]): F[A] = ev.evals(gca)
  }

  implicit final class EvalsOps[F[_], G[_], A](private val fa: F[A]) extends AnyVal {
    def evalMap[B](f: A => G[B])(implicit evals: Evals[F, G]): F[B]                      = evals.evalMap(fa)(f)
    def evalTap[B](f: A => G[B])(implicit evals: Evals[F, G], functor: Functor[G]): F[A] =
      evals.evalMap(fa)(a => f(a) as a)
  }
}
