package tofu.internal
import scala.reflect.macros._

class Interop(val c: blackbox.Context) {
  import c.universe._
  import c.{WeakTypeTag => WTT}
  type WTTU[F[_]] = WTT[F[Unit]]

  protected def tc[F[_]](implicit wttu: WTTU[F]) = wttu.tpe.typeConstructor
  protected def t[A](implicit wttu: WTT[A])      = wttu.tpe

  protected def delegateParamTree[N](ps: Seq[Seq[Tree]])(ts: Seq[Type])(implicit N: WTT[N]): Tree = {
    val s   = N.tpe.decls.head.asTerm.name.decodedName.toString
    val exp = c.parse(s)

    q"$exp[..$ts](...$ps)"
  }

  protected def delegateTree[N](ts: Type*)(implicit N: WTT[N]): Tree =
    delegateParamTree(Nil)(ts)

  private def delegateImpl[R: WTT, N: WTT](ts: Type*): c.Expr[R] =
    c.Expr[R](delegateTree[N](ts: _*))

  private def delegateParamImpl[R: WTT, N: WTT](ps: Tree*)(ts: Type*): c.Expr[R] =
    c.Expr[R](delegateParamTree[N](Seq(ps))(ts))

  def delegate[R: WTT, F[_]: WTTU, N: WTT]: c.Expr[R]                      = delegateImpl[R, N](tc[F])
  def delegate2[R: WTT, I[_]: WTTU, F[_]: WTTU, N: WTT]: c.Expr[R]         = delegateImpl[R, N](tc[I], tc[F])
  def delegate1p1[R: WTT, T: WTT, F[_]: WTTU, N: WTT](p1: Tree): c.Expr[R] = delegateParamImpl[R, N](p1)(t[T], tc[F])
}

class WBInterop(override val c: whitebox.Context) extends Interop(c) {
  import c.universe._
  import c.{WeakTypeTag => WTT}
  def delegate0[F[_]: WTTU, N: WTT]: Tree         = delegateTree[N](tc[F])
  def delegate1[F[_]: WTTU, A: WTT, N: WTT]: Tree = delegateTree[N](tc[F], t[A])
}
