import sbt._

object Builds extends sbt.Build {
  import Keys._

  lazy val root = Project(
    "dispatch", file("."), settings = Defaults.defaultSettings ++ Seq(
        unmanagedClasspath in (LocalProject("dispatch"), Test) <++=
          (fullClasspath in (scalacheck, Compile))
    )
  ) aggregate (scalacheck)

  lazy val scalacheck: ProjectReference =
    uri("git://github.com/rickynils/scalacheck.git#cac828d224")
}


