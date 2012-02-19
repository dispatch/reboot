resolvers ++= Seq(
  "less is" at "http://repo.lessis.me",
  "coda" at "http://repo.codahale.com",
  Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)
)

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.1")

addSbtPlugin("com.jsuereth" % "xsbt-gpg-plugin" % "0.5")

publishMavenStyle := true

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) 
    Some("snapshots" at nexus + "content/repositories/snapshots") 
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

licenses := Seq("LGPL v3" -> url("http://www.gnu.org/licenses/lgpl.txt"))

homepage := Some(url("http://dispatch.databinder.net/"))

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
