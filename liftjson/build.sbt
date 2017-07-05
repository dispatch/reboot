name := "dispatch-lift-json"

description :=
  "Dispatch module providing lift json support"

scalacOptions += "-Xfatal-warnings"

Seq(lsSettings :_*)

libraryDependencies ++= Seq(
  "net.liftweb" %% "lift-json" % "3.1.0",
  "org.mockito" % "mockito-core" % "1.10.19" % "test"
)
