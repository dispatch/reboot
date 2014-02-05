name := "dispatch-oauth2"

description :=
  "Dispatch module providing oauth2 support"

libraryDependencies ++= Seq(
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.0",
  "net.databinder.dispatch" %% "dispatch-json4s-native" % "0.11.0"
)
