name := "dispatch-core"

description :=
  "Core Dispatch module wrapping async-http-client"

libraryDependencies +=
  "org.asynchttpclient" % "async-http-client" % "[2.1.2,2.2.1,2.3.0]"

enablePlugins(BuildInfoPlugin)

buildInfoKeys := Seq[BuildInfoKey](version)

buildInfoPackage := "dispatch"
