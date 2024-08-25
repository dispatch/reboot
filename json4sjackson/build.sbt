name := "dispatch-json4s-jackson"

description :=
  "Dispatch module providing json4s support"

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-jackson" % "4.1.0-M5",
  "ws.unfiltered" %% "unfiltered-netty-server" % "0.13.0-M2" % "test"
)
