name := "dispatch-lift-json"

description :=
  "Dispatch module providing lift json support"

scalacOptions += "-Xfatal-warnings"

libraryDependencies ++= Seq(
  "net.liftweb" %%  "lift-json"     % Common.liftVersion,
  "org.mockito" %   "mockito-core"  % Common.mockitoVersion % Test
)

//////////////////////////////////////////////////////////////////////////////////////////////////
// TODO: lift-json is currently not available (see https://github.com/lift/framework/issues/1955)
// once it becomes available, delete these additional rules

libraryDependencies --= {
  if (scalaVersion.value != "2.13.0-M5") Nil else Seq(
    "net.liftweb" %% "lift-json" % Common.liftVersion,
  )
}

excludeFilter in unmanagedSources := {
  val default = (excludeFilter in unmanagedSources).value
  if (scalaVersion.value == "2.13.0-M5") default || "*.scala" else default
}

publishArtifact := {
  scalaVersion.value != "2.13.0-M5"
}
