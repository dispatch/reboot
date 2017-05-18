name := "dispatch-json4s-jackson"

description :=
  "Dispatch module providing json4s support"

Seq(lsSettings: _*)

libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-jackson" % "3.5.1",
  "net.databinder" %% "unfiltered-netty-server" % "0.8.4" % "test" excludeAll ExclusionRule(organization = "io.netty")
)

resolvers += "Farmdawg's Temp Forks" at "https://dl.bintray.com/farmdawgnation/temp-forks"
