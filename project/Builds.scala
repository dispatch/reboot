import ls.Plugin._
import sbt._
import sbtbuildinfo.BuildInfoPlugin
import sbtbuildinfo.BuildInfoPlugin.autoImport._

object Builds extends sbt.Build {
  import Keys._

  /** Aggregates tasks for all projects */
  lazy val root = Project("dispatch-all", file(".")).settings(
    Defaults.coreDefaultSettings,
    Common.settings,

    ls.Plugin.LsKeys.skipWrite := true,
    publish := { }
  ).aggregate(core, jsoup, tagsoup, liftjson, json4sJackson, json4sNative)

  def module(name: String) =
    Project(name,  file(name.replace("-", ""))).settings(
      Defaults.coreDefaultSettings,
      Common.settings,
      Common.testSettings,
      lsSettings
    ).dependsOn(ufcheck % "test->test")

  lazy val core = module("core").enablePlugins(BuildInfoPlugin).settings(
    xmlDependency,

    name := "dispatch-core",
    description := "Core Dispatch module wrapping sonatype/async-http-client",
    libraryDependencies += "com.ning" % "async-http-client" % "1.9.25",

    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "dispatch"
  )

  lazy val liftjson = module("lift-json").settings(
    name := "dispatch-lift-json",

    description := "Dispatch module providing lift json support",

    (skip in compile) <<= scalaVersion { sv => () => sv == "2.9.3" },

    publishArtifact <<= scalaVersion { sv => sv != "2.9.3" },

    libraryDependencies ++= Seq(
      "net.liftweb" %% "lift-json" % "2.6.2" cross CrossVersion.binaryMapped {
        // Makes update resolution happy, but since w'ere not building for 2.9.3
        // we won't end up in runtime version hell by doing this.
        case "2.9.3" => "2.9.1"
        case x => x
      },
      "org.mockito" % "mockito-core" % "1.10.19" % "test"
    )
  ).dependsOn(core, core % "test->test")

  lazy val json4sJackson = module("json4s-jackson").settings(
    name := "dispatch-json4s-jackson",
    description := "Dispatch module providing json4s support",

    libraryDependencies ++= Seq(
      "org.json4s" %% "json4s-jackson" % "3.2.11",
      "net.databinder" %% "unfiltered-netty" % "0.8.4" % "test"
    )
  ).dependsOn(core, core % "test->test")

  lazy val json4sNative = module("json4s-native").settings(
    name := "dispatch-json4s-native",

    description := "Dispatch module providing json4s native support",

    libraryDependencies ++= Seq(
      "org.json4s" %% "json4s-core" % "3.2.11",
      "org.json4s" %% "json4s-native" % "3.2.11",
      "net.databinder" %% "unfiltered-netty" % "0.8.4" % "test"
    )
  ).dependsOn(core, core % "test->test")

  lazy val jsoup = module("jsoup").settings(
    name := "dispatch-jsoup",

    description := "Dispatch module providing jsoup html parsing support",

    libraryDependencies += "org.jsoup" % "jsoup" % "1.8.2"
  ).dependsOn(core, core % "test->test")

  lazy val tagsoup = module("tagsoup").settings(
    name := "dispatch-tagsoup",

    description := "Dispatch module providing tagsoup xml and html parsing support",

    libraryDependencies += "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1"
  ).dependsOn(core, core % "test->test")

  lazy val xmlDependency = libraryDependencies <<= (libraryDependencies, scalaVersion) {
    (dependencies, scalaVersion) =>
      if(scalaVersion.startsWith("2.11"))
        ("org.scala-lang.modules" %% "scala-xml" % "1.0.4") +: dependencies
      else
        dependencies
    }

  /** Util module for using unfiltered with scalacheck */
  lazy val ufcheck = Project("ufcheck", file("ufcheck")).settings(
    Defaults.coreDefaultSettings,
    scalaVersion := Common.defaultScalaVersion,

    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck" % "1.11.3" % "test",
      "net.databinder" %% "unfiltered-netty-server" % "0.8.0" % "test",
      "org.slf4j" % "slf4j-simple" % "1.6.4" % "test"
    )
  )
}
