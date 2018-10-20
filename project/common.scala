import sbt._

object Common {
  import Keys._

  val defaultScalaVersion = "2.12.7"

  val testSettings:Seq[Setting[_]] = Seq(
    testOptions in Test += Tests.Cleanup { loader =>
      val c = loader.loadClass("unfiltered.spec.Cleanup$")
      c.getMethod("cleanup").invoke(c.getField("MODULE$").get(c))
    }
  )

  val settings: Seq[Setting[_]] = Seq(
    version := "1.1.0-SNAPSHOT",

    crossScalaVersions := Seq("2.11.12", "2.12.7"),

    scalaVersion := defaultScalaVersion,

    scalacOptions in (Compile) ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:higherKinds",
      "-language:implicitConversions",
      // "-Xfatal-warnings",
      "-Xlint",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Xfuture"
    ),

    scalacOptions in (Compile) ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, v)) if v <= 12 =>
          Seq("-Yno-adapted-args")
        case _ =>
          Nil
      }
    },

    scalacOptions in (Test) ~= { (opts: Seq[String]) =>
      opts.diff(
        Seq(
          "-Xlint"
        )
      )
    },

    organization := "org.dispatchhttp",

    homepage :=
      Some(new java.net.URL("https://dispatchhttp.org/")),

    publishMavenStyle := true,

    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (version.value.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },

    publishArtifact in Test := false,

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
