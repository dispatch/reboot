name := "dispatch-core"

description :=
  "Core Dispatch module wrapping sonatype/async-http-client"

libraryDependencies +=
  "com.ning" % "async-http-client" % "1.9.15"

Seq(lsSettings :_*)

Seq(buildInfoSettings:_*)

sourceGenerators in Compile <+= buildInfo

buildInfoKeys := Seq[BuildInfoKey](version)

buildInfoPackage := "dispatch"
