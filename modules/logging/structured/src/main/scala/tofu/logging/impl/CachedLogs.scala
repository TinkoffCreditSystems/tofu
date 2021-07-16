package tofu.logging.impl
import cats.Monad
import tofu.Guarantee
import tofu.concurrent.QVar
import tofu.logging.{LoggedValue, Logging, Logs}
import tofu.syntax.guarantee._
import tofu.syntax.monadic._

import scala.reflect.{ClassTag, classTag}

class CachedLogs[I[_]: Monad: Guarantee, F[_]](
    underlying: Logs[I, F],
    nameCache: QVar[I, Map[String, Logging[F]]],
    tagCache: QVar[I, Map[ClassTag[_], Logging[F]]]
) extends Logs[I, F] {
  private[this] case object NoneLogging extends Logging[F] {
    def write(level: Logging.Level, message: String, values: LoggedValue*): F[Unit] =
      throw new UnsupportedOperationException("CachedLogs.NonLogging should be never used")
  }

  private[this] def safeGet[K](mvar: QVar[I, Map[K, Logging[F]]], create: => I[Logging[F]], key: K): I[Logging[F]] =
    mvar.read.flatMap(_.getOrElse(key, NoneLogging) match {
      case NoneLogging =>
        mvar.take.bracketIncomplete { map =>
          map.getOrElse(key, NoneLogging) match {
            case NoneLogging => create.flatTap(logging => mvar.put(map.updated(key, logging)))
            case logging     => mvar.put(map) as logging
          }
        }(mvar.put)
      case logging     => logging.pure[I]
    })

  override def forService[Svc: ClassTag]: I[Logging[F]] = safeGet(tagCache, underlying.forService[Svc], classTag[Svc])
  def byName(name: String): I[Logging[F]]               = safeGet(nameCache, underlying.byName(name), name)
}
