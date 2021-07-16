package tofu.logging

import cats.instances.list._
import cats.instances.unit._
import tofu.syntax.monadic._
import cats.syntax.foldable._
import cats.syntax.traverse._
import scala.collection.mutable

import cats.{Applicative, Monoid}
import io.circe.Json
import cats.Eval

sealed trait LogTree {
  import LogTree._
  def json: Eval[Json] = this match {
    case dict: LogDict        =>
      dict.getList.flatMap { _.traverse { case (name, t) => t.json.map(name -> _) }.map(Json.obj(_: _*)) }
    case LogArr(items)        => items.toList.traverse(_.json).map(Json.arr(_: _*))
    case value: LogParamValue => value.jsonVal.pure[Eval]
  }
}

// pretty much unsafe mutable builder for circe.Json from logs
object LogTree extends LogBuilder[Json] {
  implicit def monoid: Monoid[Output] = Applicative.monoid

  type Output = Eval[Unit]
  type ValRes = Eval[Boolean]
  class Value(private var tree: LogTree) {
    def get                 = Eval.always(tree)
    def set(value: LogTree) = Eval.always { tree = value }
  }
  type Top = LogDict

  class LogDict(private val values: mutable.Map[String, LogTree]) extends LogTree {
    def add(name: String, tree: LogTree): Eval[Unit] = Eval.always(values.update(name, tree))
    def getList: Eval[List[(String, LogTree)]]       = Eval.always(values.toList)
  }

  final case class LogArr(values: Iterable[LogTree]) extends LogTree

  private val newdict = Eval.always(mutable.Map.empty[String, LogTree]).map(new LogDict(_))
  private val newtree = Eval.always(new Value(NullValue))

  val receiver: LogRenderer[LogDict, Value, Output, ValRes] = new LogRenderer[LogDict, Value, Output, ValRes] {

    def coalesce(f: Value => ValRes, g: Value => ValRes, v: Value): ValRes =
      f(v).flatMap(written => if (written) Eval.now(true) else g(v))
    def zero(v: Value): Eval[Boolean]                                      = Eval.now(false)
    def noop(i: LogDict): Eval[Unit]                                       = Eval.now(())
    def combine(x: Eval[Unit], y: Eval[Unit]): Eval[Unit]                  = x *> y
    def putValue(value: LogParamValue, input: Value): Eval[Boolean]        = input.set(value) as true

    def sub(name: String, input: LogDict)(receive: Value => Eval[Boolean]): Eval[Unit] =
      for {
        t <- newtree
        _ <- receive(t)
        v <- t.get
        _ <- input.add(name, v)
      } yield ()

    def list(size: Int, input: Value)(receive: (Value, Int) => Eval[Boolean]): Eval[Boolean] =
      for {
        ts <- newtree.replicateA(size)
        _  <- ts.zipWithIndex.traverse_(receive.tupled)
        vs <- ts.traverse(_.get)
        _  <- input.set(LogArr(vs))
      } yield true
    def dict(input: Value)(receive: LogDict => Eval[Unit]): Eval[Boolean]                    =
      newdict flatMap input.set as true
  }

  def buildJson(buildTree: LogDict => Output): Eval[Json] = newdict flatTap buildTree flatMap (_.json)

  def make(f: LogDict => Eval[Unit]): Json = buildJson(f).value
}

sealed trait LogParamValue extends LogTree {
  def value: Any

  def jsonVal: Json = this match {
    case StrValue(value)     => Json.fromString(value)
    case IntValue(value)     => Json.fromLong(value)
    case BigIntValue(value)  => Json.fromBigInt(value)
    case DecimalValue(value) => Json.fromBigDecimal(value)
    case FloatValue(value)   => Json.fromDoubleOrString(value)
    case BoolValue(value)    => Json.fromBoolean(value)
    case NullValue           => Json.Null
  }
}

final case class StrValue(value: String)         extends LogParamValue
final case class IntValue(value: Long)           extends LogParamValue
final case class BigIntValue(value: BigInt)      extends LogParamValue
final case class DecimalValue(value: BigDecimal) extends LogParamValue
final case class FloatValue(value: Double)       extends LogParamValue
final case class BoolValue(value: Boolean)       extends LogParamValue
case object NullValue                            extends LogParamValue {
  def value = null
}
object LogParamValue {
  def apply(x: Any): LogParamValue = x match {
    case null          => NullValue
    case x: String     => StrValue(x)
    case x: Char       => StrValue(x.toString)
    case x: Byte       => IntValue(x.toLong)
    case x: Short      => IntValue(x.toLong)
    case x: Int        => IntValue(x.toLong)
    case x: Long       => IntValue(x)
    case x: BigInt     => BigIntValue(x)
    case x: BigDecimal => DecimalValue(x)
    case x: Float      => FloatValue(x.toDouble)
    case x: Double     => FloatValue(x)
    case x: Boolean    => BoolValue(x)
    case _             => StrValue(x.toString)
  }

}
