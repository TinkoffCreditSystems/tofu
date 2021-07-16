package tofu.zioInstances

import tofu.lift.{Lift, Unlift, UnliftIO}
import tofu.optics.{Contains, Extract}
import tofu.zioInstances.implicits._
import tofu._
import zio._

import scala.annotation.nowarn

object ZioInstancesSuite {

  def summonZioInstances[E1, E2: * Extract E1, R1, R2: * Contains R1](): Unit = {
    implicitly[Lift[IO[E1, *], ZIO[R1, E1, *]]]
    implicitly[Lift[URIO[R1, *], ZIO[R1, E1, *]]]

    implicitly[Unlift[IO[E1, *], ZIO[R1, E1, *]]]
    implicitly[Unlift[ZIO[R1, E1, *], ZIO[R2, E1, *]]]

    implicitly[UnliftIO[RIO[R1, *]]]

    implicitly[ErrorsTo[IO[E1, *], IO[E1, *], E1]]
    implicitly[ErrorsTo[ZIO[R1, E1, *], ZIO[R1, E1, *], E1]]

    implicitly[ErrorsTo[IO[E1, *], UIO, E1]]
    implicitly[ErrorsTo[ZIO[R1, E1, *], URIO[R1, *], E1]]

    implicitly[ErrorsTo[IO[E1, *], IO[E2, *], E1]]
    implicitly[ErrorsTo[ZIO[R1, E1, *], ZIO[R1, E2, *], E1]]

    implicitly[RunContext[RIO[R1, *]]]
    implicitly[HasContextRun[RIO[R1, *], Task, R1]]
    implicitly[WithRun[RIO[R1, *], Task, R1]]

    implicitly[RunContext[URIO[R1, *]]]
    implicitly[HasContextRun[URIO[R1, *], UIO, R1]]
    implicitly[WithRun[URIO[R1, *], UIO, R1]]

    implicitly[RunContext[ZIO[R1, E1, *]]]
    implicitly[HasContextRun[ZIO[R1, E1, *], IO[E1, *], R1]]
    implicitly[WithRun[ZIO[R1, E1, *], IO[E1, *], R1]]
    ()
  }

  @nowarn("cat=unused-locals")
  def summonZioInstances[E, Ctx: Tag](): Unit = {
    type ZContext = Has[Ctx]

    implicitly[WithRun[ZIO[ZEnv with ZContext, E, *], ZIO[ZEnv, E, *], Ctx]]
    implicitly[WithRun[ZIO[ZContext with ZEnv, E, *], ZIO[ZEnv, E, *], Ctx]]
    implicitly[WithLocal[ZIO[ZEnv with ZContext, E, *], Ctx]]
    implicitly[WithLocal[ZIO[ZContext with ZEnv, E, *], Ctx]]
    ()
  }

  @nowarn("cat=unused-params")
  def taskAmbiguity: Any = {
    import cats.effect.Sync
    import tofu.Raise
    import tofu.zioInstances.implicits.rioTofuImplicit
    import zio.Task
    import zio.interop.catz.taskConcurrentInstance

    class SyncDependency[F[_]: Sync]

    class RaiseDependency[F[_]: Raise[*[_], RuntimeException]]
    class ErrorsDependency[F[_]: Errors[*[_], RuntimeException]]

    new SyncDependency[Task]
    new RaiseDependency[Task]
    new ErrorsDependency[Task]

    implicitly[Throws[Task]]
  }

}
