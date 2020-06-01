package tofu.optics

import cats.{Functor, Monoid, Semigroup}
import tofu.optics.data.Constant

/** aka Getter
  * A has exactly one B
  * mere function from A to B
  * and part of Lens
  */
trait PExtract[-S, +T, +A, -B] extends PDowncast[S, T, A, B] with PReduced[S, T, A, B] {
  def extract(s: S): A

  def downcast(s: S): Option[A]                       = Some(extract(s))
  def reduceMap[X: Semigroup](s: S)(f: A => X): X     = f(extract(s))
  override def foldMap[X: Monoid](s: S)(f: A => X): X = f(extract(s))
}

object Extract extends MonoOpticCompanion(PExtract)

object PExtract extends OpticCompanion[PExtract] {
  def compose[S, T, A, B, U, V](f: PExtract[A, B, U, V], g: PExtract[S, T, A, B]): PExtract[S, T, U, V] =
    s => f.extract(g.extract(s))

  trait Context extends PContains.Context {
    type X
    type F[+A] = Constant[X, A]
    def functor: Functor[Constant[X, *]] = Constant.bifunctor.rightFunctor
  }
  override def toGeneric[S, T, A, B](o: PExtract[S, T, A, B]): Optic[Context, S, T, A, B] =
    new Optic[Context, S, T, A, B] {
      def apply(c: Context)(p: A => Constant[c.X, B]): S => Constant[c.X, T] = s => p(o.extract(s)).retag
    }
  override def fromGeneric[S, T, A, B](o: Optic[Context, S, T, A, B]): PExtract[S, T, A, B] =
    s => o(new Context { type X = A })(a => Constant.Impl(a))(s).value
}
