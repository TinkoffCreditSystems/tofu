import Publish._, Dependencies._
import com.typesafe.sbt.SbtGit.git
import sbt.ModuleID

lazy val setMinorVersion = minorVersion := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, v)) => v.toInt
    case _            => 0
  }
}

lazy val defaultSettings = Seq(
  scalaVersion := Version.scala213,
  crossScalaVersions := Vector(Version.scala212, Version.scala213),
  setMinorVersion,
  defaultScalacOptions,
  scalacWarningConfig,
  Compile / doc / scalacOptions -= "-Xfatal-warnings",
  libraryDependencies ++= Seq(
    compilerPlugin(kindProjector),
    compilerPlugin(betterMonadicFor),
    scalatest,
    collectionCompat,
    scalaOrganization.value % "scala-reflect" % scalaVersion.value % Provided
  )
) ++ macros ++ simulacrumOptions

lazy val higherKindCore = project
  .in(file("modules/higherKindCore"))
  .settings(
    defaultSettings,
    name := "tofu-core-higher-kind",
    libraryDependencies ++= Seq(catsCore, catsFree, catsTagless),
  )

lazy val kernel = project
  .in(file("modules/kernel"))
  .settings(defaultSettings)
  .dependsOn(opticsCore, higherKindCore)
  .settings(
    defaultSettings,
    name := "tofu-kernel"
  )

lazy val kernelCE2Interop = project
  .in(file("modules/kernelCE2Interop"))
  .dependsOn(kernel)
  .settings(
    defaultSettings,
    name := "tofu-kernel-ce2-interop",
    libraryDependencies += catsEffect
  )

lazy val core = project
  .in(file("modules/core"))
  .dependsOn(kernel, kernelCE2Interop)
  .settings(
    defaultSettings,
    name := "tofu-core",
  )

lazy val coreCatsMtlInterop = project
  .in(file("modules/core/interop/cats-mtl"))
  .settings(
    defaultSettings,
    name := "tofu-core-cats-mtl",
    libraryDependencies += catsMtl
  )
  .dependsOn(kernel)

lazy val memo = project
  .in(file("modules/memo"))
  .dependsOn(core, concurrent)
  .settings(
    defaultSettings,
    libraryDependencies ++= Seq(catsCore, catsEffect),
    name := "tofu-memo"
  )

lazy val loggingStr = project
  .in(file("modules/logging/structured"))
  .settings(
    name := "tofu-logging-structured",
    defaultSettings,
    libraryDependencies ++= Seq(
      catsCore,
      circeCore,
      tethys,
      tethysJackson,
      slf4j,
      alleycats,
      scalatest,
      derevo,
      catsTagless
    ),
  )
  .dependsOn(kernel)

lazy val loggingDer = project
  .in(file("modules/logging/derivation"))
  .dependsOn(loggingStr)
  .dependsOn(opticsMacro % "compile->test", derivation % "compile->test")
  .settings(
    defaultSettings,
    libraryDependencies ++= Seq(derevo, magnolia, slf4j),
    name := "tofu-logging-derivation"
  )

lazy val loggingLayout = project
  .in(file("modules/logging/layout"))
  .settings(
    defaultSettings,
    libraryDependencies ++= Seq(catsCore, logback, slf4j),
    name := "tofu-logging-layout"
  )
  .dependsOn(loggingStr)

lazy val loggingUtil = project
  .in(file("modules/logging/util"))
  .settings(
    defaultSettings,
    name := "tofu-logging-util",
    libraryDependencies ++= Vector(slf4j, catsEffect),
  )
  .dependsOn(loggingStr)

lazy val loggingShapeless = project
  .in(file("modules/logging/interop/shapeless"))
  .settings(
    defaultSettings,
    name := "tofu-logging-shapeless",
    libraryDependencies += shapeless
  )
  .dependsOn(loggingStr)

lazy val loggingRefined = project
  .in(file("modules/logging/interop/refined"))
  .settings(
    defaultSettings,
    name := "tofu-logging-refined",
    libraryDependencies += refined
  )
  .dependsOn(loggingStr)

lazy val loggingLog4Cats = project
  .in(file("modules/logging/interop/log4cats"))
  .settings(
    defaultSettings,
    name := "tofu-logging-log4cats",
    libraryDependencies += log4Cats
  )
  .dependsOn(loggingStr)

lazy val logging = project
  .in(file("modules/logging"))
  .dependsOn(loggingStr, loggingDer, loggingLayout, loggingUtil, loggingShapeless, loggingRefined, loggingLog4Cats)
  .aggregate(loggingStr, loggingDer, loggingLayout, loggingUtil, loggingShapeless, loggingRefined, loggingLog4Cats)
  .settings(
    defaultSettings,
    name := "tofu-logging"
  )

lazy val env = project
  .in(file("modules/env"))
  .dependsOn(core, memo)
  .settings(
    defaultSettings,
    libraryDependencies ++= Seq(catsCore, catsEffect, monix),
    name := "tofu-env"
  )

lazy val observable = project
  .in(file("modules/observable"))
  .settings(
    defaultSettings,
    libraryDependencies ++= Vector(monix, catsEffect),
    libraryDependencies += scalatest,
    name := "tofu-observable",
  )

lazy val concurrent =
  project
    .in(file("modules/concurrent"))
    .dependsOn(core, derivation % "compile->test", opticsMacro % "compile->test")
    .settings(
      defaultSettings,
      libraryDependencies ++= Seq(catsEffect, catsTagless),
      libraryDependencies ++= Seq(simulacrum, derevoTagless).map(_ % Test),
      name := "tofu-concurrent",
    )

lazy val config = project
  .in(file("modules/config"))
  .dependsOn(core, opticsCore, concurrent)
  .settings(
    defaultSettings,
    libraryDependencies ++= Seq(typesafeConfig, magnolia, derevo),
    name := "tofu-config",
  )

lazy val opticsCore = project
  .in(file("modules/optics/core"))
  .settings(
    defaultSettings,
    libraryDependencies ++= Seq(catsCore, alleycats),
    name := "tofu-optics-core"
  )
  .dependsOn(higherKindCore)

lazy val opticsInterop = project
  .in(file("modules/optics/interop"))
  .dependsOn(opticsCore)
  .settings(defaultSettings, libraryDependencies ++= Vector(monocle, catsCore), name := "tofu-optics-interop")

lazy val opticsMacro = project
  .in(file("modules/optics/macro"))
  .dependsOn(opticsCore)
  .settings(
    defaultSettings,
    scalacOptions ~= { opts =>
      val suppressed = List(
        "unused:params",
        "unused:patvars"
      )
      opts.filterNot(opt => suppressed.exists(opt.contains))
    },
    name := "tofu-optics-macro"
  )

lazy val enums = project
  .in(file("modules/enums"))
  .dependsOn(loggingStr)
  .settings(
    defaultSettings,
    libraryDependencies ++= Seq(enumeratum),
    name := "tofu-enums",
  )

lazy val derivation =
  project
    .in(file("modules/derivation"))
    .settings(
      defaultSettings,
      libraryDependencies ++= Seq(magnolia, derevo, catsTagless),
      name := "tofu-derivation",
    )
    .dependsOn(kernel)

lazy val zioCore =
  project
    .in(file("modules/zio/core"))
    .settings(defaultSettings, libraryDependencies ++= List(zio, zioCats), name := "tofu-zio-core")
    .dependsOn(core, concurrent)

lazy val zioLogging =
  project
    .in(file("modules/zio/logging"))
    .settings(
      defaultSettings,
      libraryDependencies ++= List(zio, zioCats, slf4j, logback % Test),
      name := "tofu-zio-logging"
    )
    .dependsOn(loggingStr, loggingDer % "test", zioCore % Test)

lazy val zioInterop = project
  .in(file("modules/zio"))
  .settings(
    name := "tofu-zio-interop",
    defaultSettings
  )
  .dependsOn(zioCore, zioLogging)
  .aggregate(zioCore, zioLogging)

lazy val fs2Interop = project
  .in(file("modules/fs2"))
  .settings(
    name := "tofu-fs2-interop",
    libraryDependencies += fs2,
    defaultSettings
  )
  .dependsOn(concurrent, streams)

lazy val doobie  = project
  .in(file("modules/doobie"))
  .settings(
    libraryDependencies ++= List(doobieCore, derevo, monix % Test),
    defaultSettings,
    name := "tofu-doobie",
  )
  .dependsOn(core, derivation, env % Test, zioInterop % Test)

lazy val streams = project
  .in(file("modules/streams"))
  .settings(
    libraryDependencies ++= List(fs2 % Test),
    defaultSettings,
    name := "tofu-streams",
  )
  .dependsOn(kernel)

lazy val coreModules =
  Vector(
    higherKindCore,
    kernel,
    kernelCE2Interop,
    core,
    opticsMacro,
    memo,
    derivation,
    env,
    concurrent,
    opticsCore,
    streams,
    coreCatsMtlInterop
  )

lazy val commonModules =
  Vector(observable, opticsInterop, logging, enums, config, zioInterop, fs2Interop, doobie)

lazy val allModuleRefs = (coreModules ++ commonModules).map(x => x: ProjectReference)
lazy val allModuleDeps = (coreModules ++ commonModules).map(x => x: ClasspathDep[ProjectReference])

lazy val docs = project // new documentation project
  .in(file("tofu-docs"))
  .settings(
    noPublishSettings,
    addCompilerPlugin(simulacrum),
    macros,
    ScalaUnidoc / unidoc / scalacOptions += "-Ymacro-expand:none",
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(allModuleRefs: _*) -- inProjects(opticsMacro),
    ScalaUnidoc / unidoc / target := (LocalRootProject / baseDirectory).value / "website" / "static" / "api",
    cleanFiles += (ScalaUnidoc / unidoc / target).value,
    docusaurusCreateSite := docusaurusCreateSite.dependsOn(Compile / unidoc).value,
    docusaurusPublishGhpages := docusaurusPublishGhpages.dependsOn(Compile / unidoc).value
  )
  .dependsOn(allModuleDeps: _*)
  .enablePlugins(MdocPlugin, DocusaurusPlugin, ScalaUnidocPlugin)

lazy val tofu = project
  .in(file("."))
  .settings(
    defaultSettings,
    name := "tofu"
  )
  .aggregate((coreModules ++ commonModules :+ docs).map(x => x: ProjectReference): _*)
  .dependsOn(coreModules.map(x => x: ClasspathDep[ProjectReference]): _*)

lazy val defaultScalacOptions = scalacOptions := {
  val tpolecatOptions = scalacOptions.value

  val dropLints = Set(
    "-Ywarn-dead-code",
    "-Wdead-code" // ignore dead code paths where `Nothing` is involved
  )

  val opts = tpolecatOptions.filterNot(dropLints)

  // drop `-Xfatal-warnings` on dev and 2.12 CI
  if (!sys.env.get("CI").contains("true") || (minorVersion.value == 12))
    opts.filterNot(Set("-Xfatal-warnings"))
  else
    opts
}

lazy val scalacWarningConfig = scalacOptions += {
  // // ignore unused imports that cannot be removed due to cross-compilation
  // val suppressUnusedImports = Seq(
  //   "scala/tofu/config/typesafe.scala"
  // ).map { src =>
  //   s"src=${scala.util.matching.Regex.quote(src)}&cat=unused-imports:s"
  // }.mkString(",")

  // print warning category for fine-grained suppressing, e.g. @nowarn("cat=unused-params")
  val contextDeprecationInfo = "cat=deprecation&msg=^(.*((Has)|(With)).*)$:silent"
  val verboseWarnings        = "any:wv"

  s"-Wconf:$contextDeprecationInfo,$verboseWarnings"
}

lazy val macros = Seq(
  scalacOptions ++= { if (minorVersion.value == 13) Seq("-Ymacro-annotations") else Seq() },
  libraryDependencies ++= { if (minorVersion.value == 12) Seq(compilerPlugin(macroParadise)) else Seq() }
)

lazy val noPublishSettings =
  defaultSettings ++ Seq(publish := {}, publishArtifact := false, publishTo := None, publish / skip := true)

lazy val simulacrumOptions = Seq(
  libraryDependencies += simulacrum % Provided,
  pomPostProcess := { node =>
    import scala.xml.transform.{RewriteRule, RuleTransformer}

    new RuleTransformer(new RewriteRule {
      override def transform(node: xml.Node): Seq[xml.Node] = node match {
        case e: xml.Elem
            if e.label == "dependency" &&
              e.child.exists(child => child.label == "groupId" && child.text == simulacrum.organization) &&
              e.child.exists(child => child.label == "artifactId" && child.text.startsWith(s"${simulacrum.name}_")) =>
          Nil
        case _ => Seq(node)
      }
    }).transform(node).head
  }
)

addCommandAlias("fmt", "all tofu/scalafmtSbt tofu/scalafmtAll")
addCommandAlias("checkfmt", "all tofu/scalafmtSbtCheck tofu/scalafmtCheckAll")

addCommandAlias("preparePR", "scalafmtAll ;scalafmtSbt ;reload; githubWorkflowGenerate; clean; Test / compile")
