package tofu.logging

import cats.effect.Sync
import cats.kernel.Monoid
import cats.{Applicative, Apply, FlatMap, Functor}
import org.slf4j.LoggerFactory
import tofu.higherKind
import tofu.higherKind.RepresentableK
import tofu.syntax.monadic._

import scala.reflect.ClassTag

import Logging.loggerForService
import impl.{ContextSyncLoggingImpl, SyncLogging}

trait Logs[+I[_], F[_]] extends LogsVOps[I, F] {
  def forService[Svc: ClassTag]: I[Logging[F]]
  def byName(name: String): I[Logging[F]]

  final def biwiden[I1[a] >: I[a], F1[a] >: F[a]]: Logs[I1, F1] = this.asInstanceOf[Logs[I1, F1]]

  final def service[Svc: ClassTag]: I[ServiceLogging[F, Svc]] = forService[Svc].asInstanceOf[I[ServiceLogging[F, Svc]]]
}

object Logs {
  def apply[I[_], F[_]](implicit logs: Logs[I, F]): Logs[I, F] = logs

  private[this] val logsRepresentableAny: RepresentableK[Logs[*[_], Any]] =
    higherKind.derived.genRepresentableK[Logs[*[_], Any]]

  implicit def logsRepresentable[Y[_]]: RepresentableK[Logs[*[_], Y]] =
    logsRepresentableAny.asInstanceOf[RepresentableK[Logs[*[_], Y]]]

  def provide[I[_], F[_]]  = new Provide[I, F]
  def provideM[I[_], F[_]] = new ProvideM[I, F]

  def sync[I[_]: Sync, F[_]: Sync]: Logs[I, F] = new Logs[I, F] {
    def forService[Svc: ClassTag]: I[Logging[F]] =
      Sync[I].delay(new SyncLogging[F](loggerForService[Svc]))
    def byName(name: String): I[Logging[F]] = Sync[I].delay(new SyncLogging[F](LoggerFactory.getLogger(name)))
  }

  def withContext[I[_]: Sync, F[_]: Sync](implicit ctx: LoggableContext[F]): Logs[I, F] = {
    import ctx.loggable
    new Logs[I, F] {
      def forService[Svc: ClassTag]: I[Logging[F]] =
        Sync[I].delay(new ContextSyncLoggingImpl[F, ctx.Ctx](ctx.context, loggerForService[Svc]))
      override def byName(name: String): I[Logging[F]] =
        Sync[I].delay(new ContextSyncLoggingImpl[F, ctx.Ctx](ctx.context, LoggerFactory.getLogger(name)))
    }
  }
  def const[I[_]: Applicative, F[_]](logging: Logging[F]): Logs[I, F] = new Logs[I, F] {
    def forService[Svc: ClassTag]: I[Logging[F]] = logging.pure[I]
    def byName(name: String): I[Logging[F]]      = logging.pure[I]
  }

  def empty[I[_]: Applicative, F[_]: Applicative]: Logs[I, F] = const[I, F](Logging.empty[F])

  def combine[I[_]: Apply, F[_]: Apply](las: Logs[I, F], lbs: Logs[I, F]): Logs[I, F] = new Logs[I, F] {
    def forService[Svc: ClassTag]: I[Logging[F]] = las.forService.map2(lbs.forService)(Logging.combine[F])
    def byName(name: String): I[Logging[F]]      = las.byName(name).map2(lbs.byName(name))(Logging.combine[F])
  }

  implicit def logsMonoid[I[_]: Applicative, F[_]: Applicative]: Monoid[Logs[I, F]] = new Monoid[Logs[I, F]] {
    def empty: Logs[I, F]                                 = Logs.empty[I, F]
    def combine(x: Logs[I, F], y: Logs[I, F]): Logs[I, F] = Logs.combine(x, y)
  }

  class Provide[I[_], F[_]] {
    def apply[X: ClassTag](f: Logging[F] => X)(implicit logs: Logs[I, F], I: Functor[I]) = logs.forService[X].map(f)
  }
  class ProvideM[I[_], F[_]] {
    def apply[X: ClassTag](f: Logging[F] => I[X])(implicit logs: Logs[I, F], I: FlatMap[I]) =
      logs.forService[X].flatMap(f)
  }
}
