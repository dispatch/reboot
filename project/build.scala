import sbt._

object Builds extends sbt.Build {
  import Keys._

  /** Defines common settings for all projects */
  lazy val setup = Project("setup", file("setup"))

  /** Aggregates tasks for all projects */
  lazy val root = Project(
    "dispatch-all", file("."), settings = Defaults.defaultSettings ++ Seq(
      ls.Plugin.LsKeys.skipWrite := true
    )).delegateTo(setup).aggregate(core)

  def module(name: String) =
    Project(name, file(name))
      .delegateTo(ufcheck, setup)
      .dependsOn(ufcheck % "test->test")

  lazy val core = module("core")

  /** Util module for using unfiltered with scalacheck */
  lazy val ufcheck = Project(
    "ufcheck", file("ufcheck")
  ).dependsOn(scalacheck % "test->compile")

  lazy val scalacheck = RootProject(
    uri("git://github.com/n8han/scalacheck.git#1.8cc")
  )
}


