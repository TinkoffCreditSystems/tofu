package tofu.bi

import cats.Bifunctor
import tofu.optics.Extract
import tofu.optics.Same
import tofu.optics.Contains
import tofu.bi.lift.BiUnlift
import tofu.higherKind.bi.FunBK
import tofu.control.Bind
import tofu.optics.Equivalent

/** typeclass for access a functional environment in a bifuntor
  * @tparam X contextual error
  * @tparam C contextual result
  */
trait BiContext[F[_, _], X, C] {

  /** base F bifunctor inclusion
    */
  def bifunctor: Bifunctor[F]

  /** read the contextual value of type `C` producing declared contextual error of type `X`
    */
  def context: F[X, C]

  /** focus this context instance
    *
    * @param err error mapping in the optical form, this could be autogenerated `tofu.optics.Contains`
    * @param res context mapping in the optical form, this could be autogenerated `tofu.optics.Contains`
    * @return focused instance of context
    */
  def extract[E, A](err: Extract[X, E], res: Extract[C, A]): BiContext[F, E, A] =
    new BiContextExtractInstance[F, X, C, E, A](this, err, res)

  /** focus this context changing only the error
    *
    * @param ex error mapping in the optical form
    * @return focused instance of context
    */
  def lextraxt[A](ex: Extract[C, A]): BiContext[F, X, A] = extract(Same.id, ex)

  /** focus this context changing only the result
    *
    * @param ex error mapping in the optical for
    * @return focused instance of context
    */
  def rextract[E](ex: Extract[X, E]): BiContext[F, E, C] = extract(ex, Same.id)
}

object BiContext {
  def apply[F[_, _], X, C](implicit inst: BiContext[F, X, C]): BiContext[F, X, C] = inst
}

/** typeclass for locally modification of environment for  processess
  */
trait BiLocal[F[_, _], X, C] extends BiContext[F, X, C] {

  /** run the process in a locally modified environment
    *
    * @param fea process to run
    * @param lproj a modification of the error part
    * @param rproj a modification of the result part
    * @return process with the same semantics as `fea` but run in the modified environment
    */
  def bilocal[E, A](fea: F[E, A])(lproj: X => X, rproj: C => C): F[E, A]

  /** same as `bilocal` but modify only the result part
    */
  def local[E, A](fea: F[E, A])(proj: C => C): F[E, A] = bilocal(fea)(identity, proj)

  /** same as `bilocal` but modify only the error part
    */
  def errLocal[E, A](fea: F[E, A])(proj: X => X): F[E, A] = bilocal(fea)(proj, identity)

  /** focus this context instance
    * this will read and modify only the given parts of the context
    *
    * @param err an optic for reading from and updating a larger context error
    * @param res an optic for reading from and updating a larget context result
    * @return focused instance
    */
  def sub[E, A](err: X Contains E, res: C Contains A): BiLocal[F, E, A] =
    new BiLocalSubInstance[F, X, C, E, A](this, err, res)

  /** same as `sub` but focus on the result only
    */
  def rsub[A](cts: C Contains A): BiLocal[F, X, A] = sub(Same.id, cts)

  /** same as `sub` but focus on the error only
    */
  def lsub[E](cts: X Contains E): BiLocal[F, E, C] = sub(cts, Same.id)
}

object BiLocal {
  def apply[F[_, _], X, C](implicit inst: BiLocal[F, X, C]): BiLocal[F, X, C] = inst
}

/** typeclass relation for running processes with provided environment
  * @tparam F rich process type, that requires and has access the the environment
  * @tparam G base process type
  */
trait BiRun[F[_, _], G[_, _], X, C] extends BiLocal[F, X, C] with BiUnlift[G, F] {
  override def bifunctor: Bind[F]

  /** run a process starting from the error state for the environment
    *
    * @param fa a process to run
    * @param x environmental error
    * @return base process with provided environment
    */
  def runLeft[E, A](fa: F[E, A])(x: X): G[E, A]

  /** run a process starting from the success state for the environment
    *
    * @param fa a process to run
    * @param c environmental result
    * @return base process with provided environment
    */
  def runRight[E, A](fa: F[E, A])(c: C): G[E, A]

  /** run a process starting the environment from the state defined by disjuction
    *
    * @param fa a process to run
    * @param ctx environment
    * @return base process with provided environment
    */
  def runEither[E, A](fa: F[E, A])(ctx: Either[X, C]): G[E, A] =
    ctx match {
      case Left(x)  => runLeft(fa)(x)
      case Right(r) => runRight(fa)(r)
    }

  /** map this environment using provided equivalences to other types
    *
    * @param err an optic for mapping context error
    * @param res an optic for mapping context result
    * @return focused instance
    */
  def imap[E, A](err: X Equivalent E, res: C Equivalent A): BiRun[F, G, E, A] =
    new BiRunEqvInstance[F, G, X, C, E, A](this, err, res)

  def runRightK(c: C): F FunBK G               = FunBK[F](runRight(_)(c))
  def runLeftK(x: X): F FunBK G                = FunBK[F](runLeft(_)(x))
  def runEitherK(ctx: Either[X, C]): F FunBK G = FunBK[F](runEither(_)(ctx))

  override def bilocal[E, A](fea: F[E, A])(lproj: X => X, rproj: C => C): F[E, A] =
    bifunctor.foldWith[X, C, E, A](
      context,
      x => lift(runLeft(fea)(lproj(x))),
      c => lift(runRight(fea)(rproj(c)))
    )

  override def disclose[E, A](k: FunBK[F, G] => F[E, A]): F[E, A] =
    bifunctor.foldWith[X, C, E, A](
      context,
      x => k(FunBK.apply(runLeft(_)(x))),
      c => k(FunBK.apply(runRight(_)(c)))
    )
}
object BiRun {
  def apply[F[_, _], G[_, _], X, C](implicit inst: BiRun[F, G, X, C]): BiRun[F, G, X, C] = inst
}

class BiContextExtractInstance[F[_, _], X, C, E, A](ctx: BiContext[F, X, C], lext: Extract[X, E], rext: Extract[C, A])
    extends BiContext[F, E, A] {

  override def bifunctor: Bifunctor[F] = ctx.bifunctor

  override def context: F[E, A] = bifunctor.bimap(ctx.context)(lext.extract, rext.extract)

  override def extract[E1, A1](err: tofu.optics.Extract[E, E1], res: tofu.optics.Extract[A, A1]): BiContext[F, E1, A1] =
    ctx.extract(lext >> err, rext >> res)
}

class BiLocalSubInstance[F[_, _], X, C, E, A](ctx: BiLocal[F, X, C], lcts: Contains[X, E], rcts: Contains[C, A])
    extends BiContextExtractInstance[F, X, C, E, A](ctx, lcts, rcts) with BiLocal[F, E, A] {
  override def bilocal[E1, A1](fea: F[E1, A1])(lproj: E => E, rproj: A => A): F[E1, A1] =
    ctx.bilocal(fea)(lcts.update(_, lproj), rcts.update(_, rproj))

  override def sub[E1, A1](err: tofu.optics.Contains[E, E1], res: tofu.optics.Contains[A, A1]): BiLocal[F, E1, A1] =
    ctx.sub(lcts >> err, rcts >> res)
}

class BiRunEqvInstance[F[_, _], G[_, _], X, C, X1, C1](
    ctx: BiRun[F, G, X, C],
    leq: Equivalent[X, X1],
    req: Equivalent[C, C1]
) extends BiLocalSubInstance[F, X, C, X1, C1](ctx, leq, req) with BiRun[F, G, X1, C1] {

  override def runLeft[E, A](fa: F[E, A])(x1: X1): G[E, A] = ctx.runLeft(fa)(leq.upcast(x1))

  override def runRight[E, A](fa: F[E, A])(c1: C1): G[E, A] = ctx.runRight(fa)(req.upcast(c1))

  override def lift[E, A](fa: G[E, A]): F[E, A] = ctx.lift(fa)

  override def bifunctor: Bind[F] = ctx.bifunctor
}
