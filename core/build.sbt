name := "dispatch-core"

description :=
  "Core Dispatch module wrapping sonatype/async-http-client"

libraryDependencies ++= Seq(
  "com.ning" % "async-http-client" % "1.7.12"
)

seq(lsSettings :_*)

seq(buildInfoSettings:_*)

sourceGenerators in Compile <+= buildInfo

buildInfoKeys := Seq[BuildInfoKey](version)

buildInfoPackage := "dispatch"
