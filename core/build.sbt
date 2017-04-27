name := "dispatch-core"

description :=
  "Core Dispatch module wrapping sonatype/async-http-client"

libraryDependencies +=
  "com.ning" % "async-http-client" % "1.9.11"

Seq(lsSettings :_*)

Seq(buildInfoSettings:_*)

sourceGenerators in Compile += buildInfo.taskValue

buildInfoKeys := Seq[BuildInfoKey](version)

buildInfoPackage := "dispatch"
