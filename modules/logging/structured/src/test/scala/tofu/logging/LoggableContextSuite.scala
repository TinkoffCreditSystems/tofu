package tofu.logging

import tofu.compat.unused
import tofu.WithContext

object LoggableContextSuite {

  def summonInstance[R: Loggable, F[_]: *[_] WithContext R](): Unit = {
    LoggableContext.of[F].instance
    ()
  }

  @unused
  def summonInstanceUnambiguously[R1: Loggable, R2: Loggable, F[_]: *[_] WithContext R1: *[_] WithContext R2]()
      : Unit = {
    LoggableContext.of[F].instance[R1]
    ()
  }

}
