package tofu.optics

import alleycats.Pure
import cats.{Functor, Id}
import tofu.optics.classes.PChoice
import tofu.optics.data.{Identity, Tagged}
import scala.annotation.nowarn

trait PUpcast[-S, +T, +A, -B] extends PBase[S, T, A, B] {
  def upcast(b: B): T
}

object Upcast extends MonoOpticCompanion(PUpcast)

object PUpcast extends OpticCompanion[PUpcast] with OpticProduct[PUpcast] {

  def compose[S, T, A, B, U, V](f: PUpcast[A, B, U, V], g: PUpcast[S, T, A, B]): PUpcast[S, T, U, V] =
    v => g.upcast(f.upcast(v))

  override def product[S1, S2, T1, T2, A1, A2, B1, B2](
      f: PUpcast[S1, T1, A1, B1],
      g: PUpcast[S2, T2, A2, B2]
  ) = { case (b1, b2) => (f.upcast(b1), g.upcast(b2)) }

  class Context extends PSubset.Context {
    override type P[-x, +y] = Tagged[x, y]
    type F[+x]              = x
    def pure       = Pure[Id]
    def profunctor = PChoice[Tagged]
    def functor    = Functor[Identity]
  }
  def toGeneric[S, T, A, B](o: PUpcast[S, T, A, B]): Optic[Context, S, T, A, B] =
    new Optic[Context, S, T, A, B] {
      def apply(c: Context)(p: Tagged[A, B]): Tagged[S, T] = Tagged(o.upcast(p.value))
    }
  def fromGeneric[S, T, A, B](o: Optic[Context, S, T, A, B]): PUpcast[S, T, A, B] =
    b => o(new Context)(Tagged(b)).value

  object GenericSubtypeImpl extends Upcast[Any, Any] {
    override def upcast(b: Any): Any = b
  }

  implicit def subType[E, E1](implicit @nowarn ev: E <:< E1): Upcast[E1, E] =
    GenericSubtypeImpl.asInstanceOf[Upcast[E1, E]]
}
