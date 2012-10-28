version := "0.9.3"

crossScalaVersions :=
  Seq("2.8.1", "2.8.2", "2.9.0-1", "2.9.1", "2.9.1-1", "2.9.2")

organization := "net.databinder.dispatch"

seq(lsSettings :_*)

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
