name := "dispatch-json4s-native"

description :=
  "Dispatch module providing json4s native support"

Seq(lsSettings :_*)

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-core" % "3.2.10",
  "org.json4s" %% "json4s-native" % "3.2.10",
  "net.databinder" %% "unfiltered-netty" % "0.8.0" % "test"
)

