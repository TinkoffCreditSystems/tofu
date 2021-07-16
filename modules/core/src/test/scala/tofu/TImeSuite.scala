package tofu

import cats.data.ReaderT
import cats.effect.{Concurrent, IO, Timer}

import scala.annotation.nowarn
import scala.concurrent.ExecutionContext

@nowarn
class TimeSuite {

  implicit val ioTimer = IO.timer(ExecutionContext.global)
  implicit val ioCS    = IO.contextShift(ExecutionContext.global)

  def summon[F[_]: Timer] = {
    Clock[F]
    Sleep[F]
  }

  def timeout[F[_]: Timer: Concurrent] = {
    Timeout[F]
  }

  def io = {
    Clock[IO]
    Sleep[IO]
    Timeout[IO]
  }

  def readerIO = {
    Clock[ReaderT[IO, Unit, *]]
    Sleep[ReaderT[IO, Unit, *]]
    Timeout[ReaderT[IO, Unit, *]]
  }
}
