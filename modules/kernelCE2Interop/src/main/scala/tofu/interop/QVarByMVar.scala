package tofu.interop

import cats.effect.concurrent.MVar2
import tofu.concurrent.QVar

final case class QVarByMVar[F[_], A](mvar: MVar2[F, A]) extends QVar[F, A] {
  override def isEmpty: F[Boolean] = mvar.isEmpty
  override def put(a: A): F[Unit]  = mvar.put(a)
  override def take: F[A]          = mvar.take
  override def read: F[A]          = mvar.read
}
