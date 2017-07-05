name := "dispatch-core"

description :=
  "Core Dispatch module wrapping async-http-client"

libraryDependencies +=
  "org.asynchttpclient" % "async-http-client" % "2.0.33"

Seq(lsSettings :_*)

Seq(buildInfoSettings:_*)

sourceGenerators in Compile += buildInfo.taskValue

buildInfoKeys := Seq[BuildInfoKey](version)

buildInfoPackage := "dispatch"
