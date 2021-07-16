package tofu.concurrent

import cats.effect.Sync
import cats.effect.IO
import cats.data.ReaderT
import scala.annotation.nowarn
import org.scalatest.funsuite.AnyFunSuite
import cats.Monad
import tofu.syntax.monadic._

@nowarn
class AtomSuite extends AnyFunSuite {
  def summonInstance[I[_]: Sync, F[_]: Sync] = {
    implicitly[MakeAtom[I, F]]
  }

  test("check IO has Atom") {
    assert(MakeAtom[IO, ReaderT[IO, Unit, *]].of(1).flatMap(atomProg(_).run(())).unsafeRunSync() === ("1" -> 2))
  }

  def atomProg[F[_]: Monad](atom: Atom[F, Int]) =
    for {
      x <- atom.modify(i => (i + 1, i.toString))
      y <- atom.get
    } yield (x, y)

}
