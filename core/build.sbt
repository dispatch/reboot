name := "dispatch-core"

description :=
  "Core Dispatch module wrapping async-http-client"

libraryDependencies +=
  "org.asynchttpclient" % "async-http-client" % "2.4.9"

enablePlugins(BuildInfoPlugin)

buildInfoKeys := Seq[BuildInfoKey](version)

buildInfoPackage := "dispatch"
