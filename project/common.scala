import sbt._

object Common {
  import Keys._

  val defaultScalaVersion = "3.3.9"

  val testSettings:Seq[Setting[_]] = Seq(
    Test / testOptions += Tests.Cleanup { loader =>
      val c = loader.loadClass("unfiltered.spec.Cleanup$")
      c.getMethod("cleanup").invoke(c.getField("MODULE$").get(c))
    },
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-verbosity", "3")
  )

  val settings: Seq[Setting[_]] = Seq(
    version := "2.1.0",

    crossScalaVersions := Seq("2.13.18", "3.3.9"),

    scalaVersion := defaultScalaVersion,

    Compile / scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-Xlint",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Xsource:3"
    ),

    Compile / scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v <= 12 =>
          Seq("-Yno-adapted-args", "-Xfuture")
        case _ =>
          Nil
      }
    },

    Test / scalacOptions ~= { (opts: Seq[String]) =>
      opts.diff(
        Seq(
          "-Xlint"
        )
      )
    },

    Test / scalacOptions += "-Xsource-features:infer-override",

    organization := "org.dispatchhttp",

    homepage :=
      Some(new java.net.URI("https://dispatchhttp.org/").toURL()),

    publishMavenStyle := true,

    // Releases are staged locally and then uploaded to the Sonatype Central
    // Portal with `sonaRelease`. Snapshots go straight to the Central snapshots
    // repository.
    publishTo := {
      val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
      if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
      else localStaging.value
    },

    scmInfo := Some(
      ScmInfo(
        url("https://github.com/dispatch/reboot"),
        "scm:git:git@github.com:dispatch/reboot.git"
      )
    ),

    Test / publishArtifact := false,

    licenses := Seq("LGPL v3" -> url("https://www.gnu.org/licenses/lgpl-3.0.txt")),

    developers := List(
      Developer("n8han", "Nathan Hamblen", "", url("https://github.com/n8han")),
      Developer("farmdawgnation", "Matt Farmer", "", url("https://farmdawgnation.com"))
    )
  )
}
