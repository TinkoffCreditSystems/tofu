package tofu.concurrent
import cats.{Applicative, Functor}

import tofu.higherKind.{RepresentableK, derived}
import tofu.optics.Contains
import tofu.syntax.monadic._
import cats.data.StateT
import tofu.data.calc.CalcM
import tofu.internal.carriers._

/** a middleground between cats.concurrent.Ref and zio.Ref */
trait Atom[+F[_], A] {

  /** Obtains the current value.
    *
    * Since `Ref` is always guaranteed to have a value, the returned action
    * completes immediately after being bound.
    */
  def get: F[A]

  /** Sets the current value to `a`.Agent
    *
    * The returned action completes after the reference has been successfully set.
    *
    * Satisfies:
    *   `r.set(fa) *> r.get == fa`
    */
  def set(a: A): F[Unit]

  /** Replaces the current value with `a`, returning the previous value.
    */
  def getAndSet(a: A): F[A]

  /** Modifies the current value using the supplied update function. If another modification
    * occurs between the time the current value is read and subsequently updated, the modification
    * is retried using the new value. Hence, `f` may be invoked multiple times.
    *
    * Satisfies:
    *   `r.update(_ => a) == r.set(a)`
    */
  def update(f: A => A): F[Unit]

  /** Like `tryModify` but does not complete until the update has been successfully made.
    */
  def modify[B](f: A => (A, B)): F[B]
}

object Atom {
  implicit def representableKInstance[A]: RepresentableK[Atom[*[_], A]] = derived.genRepresentableK[Atom[*[_], A]]

  final implicit class AtomOps[F[_], A](private val self: Atom[F, A]) extends AnyVal {
    def focused[B](bInA: A Contains B)(implicit F: Functor[F]): Atom[F, B] = self match {
      case FocusedAtom(v, focus) => FocusedAtom(v, focus >> bInA)
      case _                     => FocusedAtom(self, bInA)
    }
  }

  private[Atom] case class FocusedAtom[F[_]: Functor, A, B](v: Atom[F, A], focus: Contains[A, B]) extends Atom[F, B] {
    def get: F[B]                       = v.get.map(focus.get)
    def set(b: B): F[Unit]              = v.update(focus.set(_, b))
    def getAndSet(b: B): F[B]           = v.modify(a => (focus.set(a, b), focus.get(a)))
    def update(f: B => B): F[Unit]      = v.update(focus.update(_, f))
    def modify[C](f: B => (B, C)): F[C] = v.modify { a =>
      val (b, c) = f(focus.get(a))
      (focus.set(a, b), c)
    }
  }

  def stateTAtom[F[_]: Applicative, A]: Atom[StateT[F, A, *], A] = StateTAtom()

  private case class StateTAtom[F[_]: Applicative, A]() extends Atom[StateT[F, A, *], A] {
    override def get: StateT[F, A, A]                       = StateT.get
    override def set(a: A): StateT[F, A, Unit]              = StateT.set(a)
    override def getAndSet(a: A): StateT[F, A, A]           = StateT(a1 => (a, a1).pure[F])
    override def update(f: A => A): StateT[F, A, Unit]      = StateT.modify(f)
    override def modify[B](f: A => (A, B)): StateT[F, A, B] = StateT(a => f(a).pure[F])
  }

  def calcMAtom[F[+_, +_], R, S, E]: Atom[CalcM[F, R, S, S, E, *], S] =
    calcMAtomAny.asInstanceOf[Atom[CalcM[F, R, S, S, E, *], S]]

  private class CalcMAtom[F[+_, +_], R, S, E]() extends Atom[CalcM[F, R, S, S, E, *], S] {
    def get: CalcM[F, R, S, S, E, S]                       = CalcM.get
    def set(a: S): CalcM[F, R, S, S, E, Unit]              = CalcM.set(a).void
    def getAndSet(a: S): CalcM[F, R, S, S, E, S]           = CalcM.get[S] << CalcM.set(a)
    def update(f: S => S): CalcM[F, R, S, S, E, Unit]      = CalcM.update(f).void
    def modify[B](f: S => (S, B)): CalcM[F, R, S, S, E, B] = CalcM.state(f)
  }
  private[this] object calcMAtomAny             extends CalcMAtom[Any, Any, Any, Any]
}

trait MakeAtom[I[_], F[_]] {
  def atom[A](a: A): I[Atom[F, A]]
}

object MakeAtom extends MakeAtomInterop {
  def apply[I[_], F[_]](implicit makeAtom: MakeAtom[I, F]) = new Applier[I, F](makeAtom)

  final class Applier[I[_], F[_]](private val makeAtom: MakeAtom[I, F]) extends AnyVal {
    def of[A](a: A): I[Atom[F, A]] = makeAtom.atom(a)
  }

  final implicit def interopCE3[I[_], F[_]](implicit carrier: MkAtomCE3Carrier[I, F]): MakeAtom[I, F] = carrier
}

trait MakeAtomInterop {
  final implicit def interopCE2[I[_], F[_]](implicit carrier: MkAtomCE2Carrier[I, F]): MakeAtom[I, F] = carrier
}
