import sbt._

object Common {
  import Keys._

  val defaultScalaVersion = "3.4.2"

  val testSettings:Seq[Setting[_]] = Seq(
    Test / testOptions += Tests.Cleanup { loader =>
      val c = loader.loadClass("unfiltered.spec.Cleanup$")
      c.getMethod("cleanup").invoke(c.getField("MODULE$").get(c))
    },
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaCheck, "-verbosity", "3")
  )

  val settings: Seq[Setting[_]] = Seq(
    version := "2.0.0-SNAPSHOT",

    crossScalaVersions := Seq("2.13.14", "3.4.2"),

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

    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (version.value.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },

    Test / publishArtifact := false,

    licenses := Seq("LGPL v3" -> url("http://www.gnu.org/licenses/lgpl.txt")),

    pomExtra :=
      <scm>
        <url>git@github.com:dispatch/reboot.git</url>
        <connection>scm:git:git@github.com:dispatch/reboot.git</connection>
      </scm>
      <developers>
        <developer>
          <id>n8han</id>
          <name>Nathan Hamblen</name>
          <url>http://twitter.com/n8han</url>
        </developer>
        <developer>
          <id>farmdawgnation</id>
          <name>Matt Farmer</name>
          <url>https://farmdawgnation.com</url>
        </developer>
      </developers>
  )
}
