import sbt._

object Builds extends sbt.Build {
  import Keys._

  /** Defines common settings for all projects */
  lazy val setup = Project("setup", file("setup"))

  /** Aggregates tasks for all projects */
  lazy val root = Project(
    "dispatch-all", file("."), settings = Defaults.defaultSettings ++ Seq(
      ls.Plugin.LsKeys.skipWrite := true
    )).delegateTo(setup).aggregate(core, liftjson, jsoup, tagsoup, json4sJackson, json4sNative)

  def module(name: String) =
    Project(name, file(name.replace("-", "")))
      .delegateTo(ufcheck, setup)
      .dependsOn(ufcheck % "test->test")

  lazy val core = module("core")

  lazy val liftjson = module("lift-json")
    .dependsOn(core)
    .dependsOn(core % "test->test")

  lazy val json4sJackson = module("json4s-jackson")
    .dependsOn(core)
    .dependsOn(core % "test->test")

  lazy val json4sNative = module("json4s-native")
    .dependsOn(core)
    .dependsOn(core % "test->test")

  lazy val jsoup = module("jsoup")
    .dependsOn(core)
    .dependsOn(core % "test->test")

  lazy val tagsoup = module("tagsoup")
    .dependsOn(core)
    .dependsOn(core % "test->test")
    
  /** Util module for using unfiltered with scalacheck */
  lazy val ufcheck = Project(
    "ufcheck", file("ufcheck")
  ).dependsOn(scalacheck % "test->compile")

  lazy val scalacheck = RootProject(
    uri("git://github.com/n8han/scalacheck.git#1.8cc")
  )
}


