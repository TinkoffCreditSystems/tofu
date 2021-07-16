package tofu.concurrent

import tofu.higherKind.{RepresentableK, derived}
import cats.Applicative
import tofu.Guarantee
import tofu.syntax.monadic._
import tofu.syntax.guarantee._
import tofu.internal.carriers.MkQVarCE3Carrier
import tofu.internal.carriers.MkQVarCE2Carrier

/** a middleground between cats.concurrent.MVar and zio.Queue.bounded(1) */
trait QVar[+F[_], A] {

  /** Returns `true` if the `MVar` is empty and can receive a `put`, or
    * `false` otherwise.
    *
    * Note that due to concurrent tasks, logic built in terms of `isEmpty`
    * is problematic.
    */
  def isEmpty: F[Boolean]

  /** Fills the `MVar` if it is empty, or blocks (asynchronously)
    * if the `MVar` is full, until the given value is next in
    * line to be consumed on [[take]].
    *
    * This operation is atomic.
    *
    * @return a task that on evaluation will complete when the
    *         `put` operation succeeds in filling the `MVar`,
    *         with the given value being next in line to
    *         be consumed
    */
  def put(a: A): F[Unit]

  /** Empties the `MVar` if full, returning the contained value,
    * or blocks (asynchronously) until a value is available.
    *
    * This operation is atomic.
    *
    * @return a task that on evaluation will be completed after
    *         a value was retrieved
    */
  def take: F[A]

  /** Tries reading the current value, or blocks (asynchronously)
    * until there is a value available.
    *
    * This operation is atomic.
    *
    * @return a task that on evaluation will be completed after
    *         a value has been read
    */
  def read: F[A]
}

object QVar {
  implicit def representableK[A]: RepresentableK[QVar[*[_], A]] = derived.genRepresentableK[QVar[*[_], A]]

  final implicit class QVarOps[F[_], A](private val self: QVar[F, A]) extends AnyVal {
    def toAtom(implicit F: Applicative[F], FG: Guarantee[F]): Atom[F, A] = QAtom(self)
  }
}

trait MakeQVar[I[_], F[_]] {
  def qvarOf[A](a: A): I[QVar[F, A]]
  def qvarEmpty[A]: I[QVar[F, A]]
}

object QVars {
  def apply[F[_]](implicit qvars: MakeQVar[F, F]): MakeQVar.Applier[F, F] = new MakeQVar.Applier(qvars)
}

object MakeQVar extends MakeQVarInterop {
  def apply[I[_], F[_]](implicit mkvar: MakeQVar[I, F]) = new Applier[I, F](mkvar)

  final class Applier[I[_], F[_]](private val makeMVar: MakeQVar[I, F]) extends AnyVal {
    def empty[A]: I[QVar[F, A]]    = makeMVar.qvarEmpty[A]
    def of[A](a: A): I[QVar[F, A]] = makeMVar.qvarOf(a)
  }

  final implicit def interopCE3[I[_], F[_]](implicit carrier: MkQVarCE3Carrier[I, F]): MakeQVar[I, F] = carrier

}

trait MakeQVarInterop {
  final implicit def interopCE2[I[_], F[_]](implicit carrier: MkQVarCE2Carrier[I, F]): MakeQVar[I, F] = carrier
}

final case class QAtom[F[_]: Applicative: Guarantee, A](qvar: QVar[F, A]) extends Atom[F, A] {
  def get: F[A]                       = qvar.read
  def set(a: A): F[Unit]              = getAndSet(a).void
  def getAndSet(a: A): F[A]           = qvar.take.guaranteeAlways(qvar.put(a))
  def update(f: A => A): F[Unit]      = qvar.take.bracketIncomplete(a => qvar.put(f(a)))(qvar.put)
  def modify[B](f: A => (A, B)): F[B] = qvar.take.bracketIncomplete { a =>
    val (next, res) = f(a)
    qvar.put(next) as res
  }(qvar.put)
}
