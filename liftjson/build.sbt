name := "dispatch-lift-json"

description :=
  "Dispatch module providing lift json support"

scalacOptions += "-Xfatal-warnings"

libraryDependencies ++= Seq(
  "net.liftweb" %% "lift-json" % "3.3.0",
  "org.mockito" % "mockito-core" % "3.0.0" % "test"
)
