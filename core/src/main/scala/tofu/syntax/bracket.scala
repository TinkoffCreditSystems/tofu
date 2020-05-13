package tofu.syntax

import cats.Applicative
import cats.data.EitherT
import cats.effect.concurrent.MVar
import cats.effect.{Bracket, ExitCase}
import cats.effect.syntax.bracket._
import cats.syntax.functor._
import cats.syntax.applicative._
import cats.syntax.either._
import tofu.{Finally, Guarantee}

object bracket {
  implicit final class TofuBracketOps[F[_], A](private val fa: F[A]) extends AnyVal {
    def bracketIncomplete[B, C](
        use: A => F[B]
    )(release: A => F[C])(implicit F: Applicative[F], FG: Guarantee[F]): F[B] =
      FG.bracket(fa)(use) { case (a, success) => release(a).whenA(!success) }

    def bracketAlways[B, C](
        use: A => F[B]
    )(release: A => F[C])(FG: Guarantee[F]): F[B] =
      FG.bracket(fa)(use) { case (a, _) => release(a) }

    def guaranteeIncomplete[B](release: F[B])(implicit F: Applicative[F], FG: Guarantee[F]): F[A] =
      FG.bracket(F.unit)(_ => fa)((_, success) => release.whenA(!success))

    def guaranteeAlways[B](release: F[B])(implicit F: Applicative[F], FG: Guarantee[F]): F[A] =
      FG.bracket(F.unit)(_ => fa)((_, _) => release)

    def bracketOpt[B, C](use: A => F[B])(release: (A, Boolean) => F[C])(implicit FG: Guarantee[F]): F[B] =
      FG.bracket(fa)(use)(release)

    def finallyCase[Ex[_], B, C](use: A => F[B])(release: (A, Ex[B]) => F[C])(implicit FG: Finally[F, Ex]): F[B] =
      FG.finallyCase(fa)(use)(release)
  }

  implicit final class TofuBracketMVarOps[F[_], A](private val mvar: MVar[F, A]) extends AnyVal {

    /**
      * Update value with effectful transformation. In case of error value remains unchanged
      * @param use function to atomically modify value contained in `MVar`
      * @return `F[A]` modified value contained in `MVar`
      */
    def bracketUpdate[A, E](use: A => F[A])(implicit F: Applicative[F], FG: Guarantee[F]): F[A] =
      mvar.take.bracketAlways(use)(mvar.put)
  }

  implicit final class TofuBracketEitherTOps[F[_], E, A](private val e: EitherT[F, E, A]) extends AnyVal {

    /** special bracket form that could handle both Either logic error and F underlying error */
    def bracketCaseErr[U, B](
        use: A => EitherT[F, E, B]
    )(release: (A, ExitCase[Either[E, U]]) => F[Unit])(implicit bracket: Bracket[F, U]): EitherT[F, E, B] =
      EitherT(
        e.value.bracketCase[Either[E, B]] {
          //could not acquire resource
          case Left(err) => bracket.pure(err.asLeft[B])
          //case logic error
          case Right(res) => use(res).leftSemiflatMap(e => release(res, ExitCase.error(e.asLeft[U])) as e).value
        }((res, cas) =>
          res match {
            case Left(_) => bracket.unit
            case Right(v) =>
              cas match {
                //case F underlying error
                case ExitCase.Error(e)  => release(v, ExitCase.error(e.asRight[E]))
                case ExitCase.Canceled  => release(v, ExitCase.Canceled)
                case ExitCase.Completed => release(v, ExitCase.Completed)
              }
          }
        )
      )

    /** special bracket form that could handle both Either logic error and F underlying error */
    def bracketIncompleteErr[U, B](
        use: A => EitherT[F, E, B]
    )(release: A => F[Unit])(implicit bracket: Bracket[F, U]): EitherT[F, E, B] =
      bracketCaseErr[U, B](use) { (a, cas) =>
        cas match {
          case ExitCase.Completed => bracket.unit
          case _                  => release(a)
        }
      }
  }
}
