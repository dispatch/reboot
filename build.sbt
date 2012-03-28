seq(lsSettings :_*)

name := "core"

description :=
  "Core Dispatch module wrapping sonatype/async-http-client"

libraryDependencies ++= Seq(
  "com.ning" % "async-http-client" % "1.7.1",
  "net.databinder" %% "unfiltered-netty-server" % "0.6.1" % "test",
  "org.slf4j" % "slf4j-simple" % "1.6.4" % "test"
)

testOptions in Test += Tests.Cleanup { loader =>
  val c = loader.loadClass("unfiltered.spec.Cleanup$")
  c.getMethod("cleanup").invoke(c.getField("MODULE$").get(c))
}

crossScalaVersions :=
  Seq("2.8.0", "2.8.1", "2.8.2", "2.9.0", "2.9.0-1", "2.9.1")

version := "0.9.0-alpha3"

organization := "net.databinder.dispatch"

homepage :=
  Some(new java.net.URL("http://dispatch.databinder.net/"))

publishMavenStyle := true

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) 
    Some("snapshots" at nexus + "content/repositories/snapshots") 
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

licenses := Seq("LGPL v3" -> url("http://www.gnu.org/licenses/lgpl.txt"))

pomExtra := (
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
  </developers>)
