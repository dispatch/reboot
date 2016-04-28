name := "dispatch-lift-json"

description :=
  "Dispatch module providing lift json support"

Seq(lsSettings :_*)

libraryDependencies ++= Seq(
  "net.liftweb" %% "lift-json" % "2.6.3",
  "org.mockito" % "mockito-core" % "1.10.19" % "test"
)
