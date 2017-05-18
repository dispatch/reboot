libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.11.6" % "test",
  "ws.unfiltered" %% "unfiltered-netty-server" % "0.9.0" % "test" excludeAll ExclusionRule(organization = "io.netty"),
  "org.slf4j" % "slf4j-simple" % "1.7.22" % "test"
)
