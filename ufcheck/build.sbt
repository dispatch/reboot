libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.11.6" % "test",
  "net.databinder" %% "unfiltered-netty-server" % "0.8.4" % "test" excludeAll ExclusionRule(organization = "io.netty"),
  "org.slf4j" % "slf4j-simple" % "1.7.22" % "test"
)

resolvers += "Farmdawg's Temp Forks" at "https://dl.bintray.com/farmdawgnation/temp-forks"
