name := "dispatch-json4s-jackson"

description :=
  "Dispatch module providing json4s support"

Seq(lsSettings: _*)

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-jackson" % "3.5.1",
  "ws.unfiltered" %% "unfiltered-netty-server" % "0.9.1" % "test" excludeAll ExclusionRule(organization = "io.netty")
)

