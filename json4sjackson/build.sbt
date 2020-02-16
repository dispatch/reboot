name := "dispatch-json4s-jackson"

description :=
  "Dispatch module providing json4s support"

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-jackson" % "3.6.7",
  "ws.unfiltered" %% "unfiltered-netty-server" % "0.10.0-M6" % "test"
)
