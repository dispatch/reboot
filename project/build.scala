import sbt._

object Builds extends sbt.Build {
  import Keys._

  /** Defines common settings for all projects */
  lazy val setup = Project("setup", file("setup"))

  /** Aggregates tasks for all projects */
  lazy val root = Project(
    "dispatch-all", file("."), settings = Defaults.defaultSettings ++ Seq(
      ls.Plugin.LsKeys.skipWrite := true,
      testOptions in Test := Nil
    )).aggregate(core).delegateTo(setup)

  lazy val core = Project(
    "core", file("core")
  ).delegateTo(setup).dependsOn(scalacheck % "test->compile")

  lazy val scalacheck = RootProject(
    uri("git://github.com/n8han/scalacheck.git#1.8cc")
  )
}


