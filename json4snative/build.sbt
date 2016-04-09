name := "dispatch-json4s-native"

description :=
  "Dispatch module providing json4s native support"

Seq(lsSettings :_*)

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-core" % "3.3.0",
  "org.json4s" %% "json4s-native" % "3.3.0",
  "net.databinder" %% "unfiltered-netty" % "0.8.4" % "test"
)

