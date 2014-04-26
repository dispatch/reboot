name := "dispatch-json4s-native"

description :=
  "Dispatch module providing json4s native support"

seq(lsSettings :_*)

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-core" % "3.2.9",
  "org.json4s" %% "json4s-native" % "3.2.9",
  "net.databinder" %% "unfiltered-netty" % "0.7.1" % "test"
)

