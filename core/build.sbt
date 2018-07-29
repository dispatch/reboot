name := "dispatch-core"

description :=
  "Core Dispatch module wrapping async-http-client"

libraryDependencies +=
  "org.asynchttpclient" % "async-http-client" % "2.5.2"

enablePlugins(BuildInfoPlugin)

buildInfoKeys := Seq[BuildInfoKey](version)

buildInfoPackage := "dispatch"
