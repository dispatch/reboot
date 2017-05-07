name := "dispatch-core"

description :=
  "Core Dispatch module wrapping sonatype/async-http-client"

libraryDependencies +=
  "org.asynchttpclient" % "async-http-client" % "2.1.0-alpha17"

Seq(lsSettings :_*)

Seq(buildInfoSettings:_*)

sourceGenerators in Compile += buildInfo.taskValue

buildInfoKeys := Seq[BuildInfoKey](version)

buildInfoPackage := "dispatch"
