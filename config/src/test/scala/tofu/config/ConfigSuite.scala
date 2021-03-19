package tofu.config
import cats.syntax.option._
import com.typesafe.config.ConfigFactory
import derevo.derive
import org.scalatest.{FlatSpec, Matchers}
import tofu.config.ConfigError.{BadNumber, BadType, NotFound}
import tofu.config.ConfigItem.ValueType

import ConfigSuite.{Foe, Skill}
import Key.{Index, Prop}

class ConfigSuite extends FlatSpec with Matchers {
  import ConfigSuite.{tryParse, fallenParse}

  "sync parsing" should "parse lists" in {
    tryParse[List[Int]]("numbers") shouldBe List(1, 2, 3)
  }

  it should "parse objects" in {
    tryParse[Foe]("foe1") shouldBe Foe(
      name = "humonoid",
      hp = 10000.toShort.some,
      energy = 1000.toShort.some,
      skills = List(Skill("zanudstvo"))
    )
  }

  it should "fail to parse incorrect types" in {
    fallenParse[Foe]("foe2") shouldBe Right(
      Vector(
        ConfigParseMessage(Vector(Prop("hp")), BadType(List(ValueType.Num), ValueType.Str))
      )
    )
  }

  it should "report correct places of errors" in {
    fallenParse[List[Foe]]("foes") shouldBe Right(
      Vector(
        ConfigParseMessage(Vector(Index(0), Prop("name")), NotFound),
        ConfigParseMessage(Vector(Index(1), Prop("name")), BadType(List(ValueType.Str), ValueType.Num)),
        ConfigParseMessage(
          Vector(Index(1), Prop("skills"), Index(0), Prop("energy")),
          BadNumber(BigDecimal("1000000000"), "bad short value")
        ),
        ConfigParseMessage(Vector(Index(2), Prop("name")), NotFound)
      )
    )
  }

}

object ConfigSuite {
  @derive(Configurable)
  final case class Foe(
      name: String,
      hp: Option[Short] = None,
      energy: Option[Short] = None,
      skills: List[Skill] = List(),
  )

  @derive(Configurable)
  final case class Skill(
      name: String,
      energy: Option[Short] = None,
  )

  val cfg = ConfigFactory.parseResources("suite.conf")

  def tryParse[A: Configurable](path: String): A =
    typesafe.syncParseValue[A](cfg.getValue(path)).getOrElse(throw new RuntimeException("no parse"))

  def fallenParse[A: Configurable](path: String): Either[A, MessageList] =
    typesafe.syncParseValue[A](cfg.getValue(path)).swap
}
