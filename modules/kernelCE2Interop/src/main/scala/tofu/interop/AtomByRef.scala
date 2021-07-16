package tofu.interop

import cats.effect.concurrent.Ref
import tofu.concurrent.Atom

final case class AtomByRef[F[_], A](ref: Ref[F, A]) extends Atom[F, A] {
  override def get: F[A]                       = ref.get
  override def set(a: A): F[Unit]              = ref.set(a)
  override def getAndSet(a: A): F[A]           = ref.getAndSet(a)
  override def update(f: A => A): F[Unit]      = ref.update(f)
  override def modify[B](f: A => (A, B)): F[B] = ref.modify(f)
}
