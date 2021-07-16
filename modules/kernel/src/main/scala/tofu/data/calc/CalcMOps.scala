package tofu.data.calc

import cats.Monad
import tofu.control.Bind
import tofu.higherKind.bi.FunBK

import cats.data.IndexedState
import tofu.optics.PContains

import tofu.compat.uv212
import tofu.optics.functions._

class CalcMOps[+F[+_, +_], -R, -SI, +SO, +E, +A] { self: CalcM[F, R, SI, SO, E, A] =>
  def widenF[F1[+x, +y] >: F[x, y] @uv212]: CalcM[F1, R, SI, SO, E, A] = this

  def mapK[G[+_, +_]](fk: F FunBK G): CalcM[G, R, SI, SO, E, A] = translate(ITranslator.mapK(fk))

  def trans: TranslatePack[F, R, SI, SO, E, A] = new TranslatePack(this)

  def translateForget[G[+_, +_], ST, R1](
      translator: Translator[F, G, ST, R, R1]
  )(implicit evs: Unit <:< SI): CalcM[G, R1, ST, ST, E, A] =
    CalcM.update((st: ST) => (st, (): SI)) *>> translateState(translator).mapState(_._1)

  def local[R1](f: R1 => R): CalcM[F, R1, SI, SO, E, A] = CalcM.read[SI, R1].flatMapS(r1 => provide(f(r1)))

  def supply(si: => SI): CalcM[F, R, Any, SO, E, A] = CalcM.set(si) *>> this

  def contramapState[SI1](f: SI1 => SI): CalcM[F, R, SI1, SO, E, A] = CalcM.update(f) *>> this

  def mapState[SO1](f: SO => SO1): CalcM[F, R, SI, SO1, E, A] =
    bind(Continue.update(f))

  def dimapState[SI1, SO1](f: SI1 => SI, g: SO => SO1): CalcM[F, R, SI1, SO1, E, A] =
    contramapState(f).mapState(g)

  def bind[F1[+x, +y] >: F[x, y] @uv212, R1 <: R, X, S, B](
      continue: Continue[A, E, SO, CalcM[F1, R1, SO, S, X, B]]
  ): CalcM[F1, R1, SI, S, X, B] =
    CalcM.Bound(this, continue)

  def foldWith[F1[+x, +y] >: F[x, y] @uv212, R1 <: R, X, S, B](
      f: A => CalcM[F1, R1, SO, S, X, B],
      h: E => CalcM[F1, R1, SO, S, X, B]
  ): CalcM[F1, R1, SI, S, X, B] = bind(Continue(f, h))

  def flatMap[F1[+x, +y] >: F[x, y] @uv212, R1 <: R, SO1 >: SO, E1 >: E, B](
      f: A => CalcM[F1, R1, SO, SO1, E1, B]
  ): CalcM[F1, R1, SI, SO1, E1, B] =
    bind(Continue.flatMapConst[A, E, SO, CalcM[F1, R1, SO, SO1, E1, B]](f))

  def flatTap[F1[+x, +y] >: F[x, y] @uv212, R1 <: R, SO1 >: SO, E1 >: E, B](
      f: A => CalcM[F1, R1, SO, SO1, E1, B]
  ): CalcM[F1, R1, SI, SO1, E1, A] =
    flatMap(a => f(a) as a)

  def biflatten[F1[+x, +y] >: F[x, y] @uv212, R1 <: R, S, X, B](implicit
      evA: A <:< CalcM[F1, R1, SO, S, X, B],
      evE: E <:< CalcM[F1, R1, SO, S, X, B]
  ): CalcM[F1, R1, SI, S, X, B] = bind(Continue.biflatten)

  def >>=[F1[+x, +y] >: F[x, y] @uv212, R1 <: R, E1 >: E, SO1 >: SO, B](f: A => CalcM[F1, R1, SO, SO1, E1, B]) =
    flatMap(f)
  def >>[F1[+x, +y] >: F[x, y] @uv212, R1 <: R, E1 >: E, SO1 >: SO, B](c: => CalcM[F1, R1, SO, SO1, E1, B])    =
    flatMap(_ => c)
  def <<[F1[+x, +y] >: F[x, y] @uv212, R1 <: R, E1 >: E, SO1 >: SO, B](
      c: => CalcM[F1, R1, SO, SO1, E1, B]
  ): CalcM[F1, R1, SI, SO1, E1, A]                                                                             = flatTap(_ => c)
  def map[B](f: A => B): CalcM[F, R, SI, SO, E, B]                                                             = flatMap(a => CalcM.Pure(f(a)))

  def handleWith[F1[+x, +y] >: F[x, y] @uv212, E1, R1 <: R, SO1 >: SO, A1 >: A](
      f: E => CalcM[F1, R1, SO, SO1, E1, A1]
  ): CalcM[F1, R1, SI, SO1, E1, A1] =
    bind(Continue.handleWithConst[A, E, SO, CalcM[F1, R1, SO, SO1, E1, A1]](f))

  def handle[A1 >: A](f: E => A1): CalcM[F, R, SI, SO, E, A1] = handleWith(e => CalcM.Pure(f(e)))

  def as[B](b: => B): CalcM[F, R, SI, SO, E, B]            = map(_ => b)
  def void: CalcM[F, R, SI, SO, E, Unit]                   = as_(())
  def as_[B](b: B): CalcM[F, R, SI, SO, E, B]              = map(_ => b)
  def mapError[E1](f: E => E1): CalcM[F, R, SI, SO, E1, A] = handleWith(e => CalcM.raise(f(e)))

  def errorAs[X](e: => X): CalcM[F, R, SI, SO, X, A]        = mapError(_ => e)
  def errorAs_[X](b: X): CalcM[F, R, SI, SO, X, A]          = mapError(_ => b)
  def provideSet(r: R, s: SI): CalcM[F, Any, Any, SO, E, A] = CalcM.Set(s) *>> provide(r)
  def provide(r: R): CalcM[F, Any, SI, SO, E, A]            = CalcM.Provide(r, this)

  def focus[S3, S4](lens: PContains[S3, S4, SI, SO]): CalcM[F, R, S3, S4, E, A] =
    CalcM.get[S3].flatMapS { s3 =>
      CalcM.set(lens.extract(s3)) *>> this.bind(Continue.focus(s3, lens))
    }

  def focusFirst[S]: CalcM[F, R, (SI, S), (SO, S), E, A]  = focus(firstP)
  def focusSecond[S]: CalcM[F, R, (S, SI), (S, SO), E, A] = focus(secondP)

  def provideSome[R1](f: R1 => R): CalcM[F, R1, SI, SO, E, A] = local(f)

  final def flatMapS[F1[+x, +y] >: F[x, y] @uv212, R1 <: R, S, E1, B](
      f: A => CalcM[F1, R1, SO, S, E1, B]
  )(implicit ev: E <:< Nothing): CalcM[F1, R1, SI, S, E1, B] =
    foldWith(f, ev)

  final def flatTapS[F1[+x, +y] >: F[x, y] @uv212, R1 <: R, S, E1, B](
      f: A => CalcM[F1, R1, SO, S, E1, B]
  )(implicit ev: E <:< Nothing): CalcM[F1, R1, SI, S, E1, A] =
    foldWith(a => f(a) as_ a, ev)

  final def productRS[F1[+x, +y] >: F[x, y] @uv212, R1 <: R, S, B, E1](
      r: => CalcM[F1, R1, SO, S, E1, B]
  )(implicit ev: E <:< Nothing): CalcM[F1, R1, SI, S, E1, B] =
    flatMapS(_ => r)

  final def productLS[F1[+x, +y] >: F[x, y] @uv212, R1 <: R, S, B, E1](
      r: => CalcM[F1, R1, SO, S, E1, B]
  )(implicit ev: E <:< Nothing): CalcM[F1, R1, SI, S, E1, A] =
    flatTapS(_ => r)

  def *>>[F1[+x, +y] >: F[x, y] @uv212, R1 <: R, S, B, E1](r: => CalcM[F1, R1, SO, S, E1, B])(implicit
      ev: E <:< Nothing
  ): CalcM[F1, R1, SI, S, E1, B] = productRS(r)

  def <<*[F1[+x, +y] >: F[x, y] @uv212, R1 <: R, S, B, E1](r: => CalcM[F1, R1, SO, S, E1, B])(implicit
      ev: E <:< Nothing
  ): CalcM[F1, R1, SI, S, E1, A] = productLS(r)

  def handleWithU[F1[+x, +y] >: F[x, y] @uv212, R1 <: R, E1, S3, B](
      f: E => CalcM[F1, R1, SO, S3, E1, B]
  )(implicit ev: A <:< Nothing): CalcM[F1, R1, SI, S3, E1, B] =
    foldWith(ev, f)

  def onErrorU[F1[+x, +y] >: F[x, y] @uv212, R1 <: R, E1, S3, B](
      f: E => CalcM[F1, R1, SO, S3, E1, B]
  )(implicit ev: A <:< Nothing): CalcM[F1, R1, SI, S3, E, B] =
    foldWith(ev, x => f(x) errorAs_ x)

  def !>>[F1[+x, +y] >: F[x, y] @uv212, R1 <: R, E1, S3, B](
      r: => CalcM[F1, R1, SO, S3, E1, B]
  )(implicit ev: A <:< Nothing): CalcM[F1, R1, SI, S3, E1, B] =
    handleWithU(_ => r)

  def <<![F1[+x, +y] >: F[x, y] @uv212, R1 <: R, E1, S3, B](
      r: => CalcM[F1, R1, SO, S3, E1, B]
  )(implicit ev: A <:< Nothing): CalcM[F1, R1, SI, S3, E, B] =
    onErrorU(_ => r)

  def swap: CalcM[F, R, SI, SO, A, E] = bind(Continue.swap)

  def when[S >: SO <: SI](b: Boolean): CalcM[F, R, S, S, E, Any] =
    if (b) this else CalcM.unit[S]

  def step(r: R, init: SI): StepResult[F, SO, E, A] = CalcM.step[F, R, SI, SO, E, A](this, r, init)

  def runTailRec[F1[+x, +y] >: F[x, y] @uv212](r: R, init: SI)(implicit F: Bind[F1]): F1[(SO, E), (SO, A)] = {
    type It = CalcM[F1, Any, Any, SO, E, A]
    F.foldRec[It, It, (SO, E), (SO, A)](Right(this.provideSet(r, init).widenF[F1])) { c =>
      c.merge.step((), ()) match {
        case StepResult.Ok(s, a)                             => F.pure(Right((s, a)))
        case StepResult.Error(s, e)                          => F.raise(Right((s, e)))
        case wrap: StepResult.Wrap[F1, r, s, SO, x, E, m, A] => F.bimap(wrap.provided)(Left(_), Left(_))
      }
    }
  }

  def runTailRecSingle[F1[+y] >: F[Any, y] @uv212, E1 >: E](r: R, init: SI)(implicit
      F: Monad[F1],
      ev: E1 <:< Nothing
  ): F1[(SO, A)] = {
    type F2[+x, +y] = F1[y]
    F.tailRecM[CalcM[F2, Any, Any, SO, Nothing, A], (SO, A)](
      this.provideSet(r, init).mapError(ev.apply).widenF[F2]
    ) { s =>
      s.step((), ()) match {
        case StepResult.Ok(s, a)                                   => F.pure(Right((s, a)))
        case err: StepResult.Error[SO, Nothing]                    => err.error
        case wrap: StepResult.Wrap[F2, r, s, SO, x, Nothing, m, A] =>
          F.map(wrap.inner)(m => Left(wrap.cont.success(wrap.state, m).provideSet(wrap.input, wrap.state)))
      }
    }
  }

  // run endofunctorial CalcT using monadic tailrec
  def tailrecEither[F1[+y] >: F[Any, y] @uv212](r: R, init: SI)(implicit F: Monad[F1]): F1[(SO, Either[E, A])] =
    F.tailRecM(step(r, init)) {
      case StepResult.Ok(state, value)                => F.pure(Right((state, Right(value))))
      case StepResult.Error(state, err)               => F.pure(Right((state, Left(err))))
      case StepResult.Wrap(input, state, inner, cont) =>
        F.map(inner)(x => Left(cont.success(state, x).step(input, state)))
    }

  def stepUnit(init: SI)(implicit ev: Unit <:< R): StepResult[F, SO, E, A] = step((), init)

  def run(r: R, init: SI)(implicit runner: CalcRunner[F]): (SO, Either[E, A]) = runner.runPair(this)(r, init)

  def runSuccess(r: R, init: SI)(implicit runner: CalcRunner[F], ev: E <:< Nothing): (SO, A) =
    run(r, init) match {
      case (s, Right(x))     => (s, x)
      case (_, Left(absurd)) => absurd
    }

  def runUnit(init: SI)(implicit runner: CalcRunner[F], evr: Unit <:< R): (SO, Either[E, A]) = run((), init)

  def results(implicit runner: CalcRunner[F], evr: Unit <:< R, evs: Unit <:< SI): (SO, Either[E, A]) = run((), ())

  def result(implicit runner: CalcRunner[F], evr: Unit <:< R, evs: Unit <:< SI): Either[E, A] = run((), ())._2

  def values(implicit runner: CalcRunner[F], evr: Unit <:< R, evs: Unit <:< SI, ev: E <:< Nothing): (SO, A) =
    runSuccess((), ())

  def value(implicit runner: CalcRunner[F], evr: Unit <:< R, evs: Unit <:< SI, ev: E <:< Nothing): A =
    runSuccess((), ())._2

  def runSuccessUnit(init: SI)(implicit runner: CalcRunner[F], ev: E <:< Nothing, evr: Unit <:< R): (SO, A) =
    runSuccess((), init)

  def toState[SI1 <: SI, SO1 >: SO, A1 >: A](implicit
      runner: CalcRunner[F],
      ev: E <:< Nothing,
      evr: Unit <:< R
  ): IndexedState[SI1, SO1, A1] =
    IndexedState(runSuccessUnit)

  def narrowRead[R1 <: R]: CalcM[F, R1, SI, SO, E, A] = this
}
