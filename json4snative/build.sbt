name := "dispatch-json4s-native"

description :=
  "Dispatch module providing json4s native support"

Seq(lsSettings :_*)

val json4sVersion = "3.5.1"

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-core" % json4sVersion,
  "org.json4s" %% "json4s-native" % json4sVersion,
  "net.databinder" %% "unfiltered-netty-server" % "0.8.4" % "test" excludeAll ExclusionRule(organization = "io.netty")
)

resolvers += "Farmdawg's Temp Forks" at "https://dl.bintray.com/farmdawgnation/temp-forks"
