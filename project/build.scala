import sbt._

object Builds extends sbt.Build {
  import Keys._

  /** Defines common settings for all projects */
  lazy val setup = Project(
    "setup",
    file("setup"),
    settings = Defaults.defaultSettings ++ Seq(
      unmanagedClasspath in (LocalProject("core"), Test) <++=
        (fullClasspath in (scalacheck, Compile))
    )
  ) delegateTo(scalacheck)
  /** Aggregates tasks for all projects */
  lazy val root = Project(
    "dispatch-all", file("."), settings = Defaults.defaultSettings ++ Seq(
      ls.Plugin.LsKeys.skipWrite := true,
      name := "Dispatch"
    )) aggregate(core)

  lazy val core = Project("core", file("core")) delegateTo (setup)

  lazy val scalacheck: ProjectReference =
    uri("git://github.com/n8han/scalacheck.git#1.8cc")
}


