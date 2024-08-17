name := "dispatch-json4s-native"

description :=
  "Dispatch module providing json4s native support"

val json4sVersion = "3.6.7"

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-core" % json4sVersion,
  "org.json4s" %% "json4s-native" % json4sVersion,
  "ws.unfiltered" %% "unfiltered-netty-server" % "0.13.0-M2" % "test"
)
