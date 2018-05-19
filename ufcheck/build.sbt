libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.14.0" % "test",
  "ws.unfiltered" %% "unfiltered-netty-server" % "0.9.1" % "test" excludeAll ExclusionRule(organization = "io.netty"),
  "org.slf4j" % "slf4j-simple" % "1.7.22" % "test"
)
