/** Aggregates tasks for all projects */
lazy val root = Project(
  "dispatch-all", file("."), settings =
    Defaults.coreDefaultSettings ++ Common.settings ++ Seq(
      ls.Plugin.LsKeys.skipWrite := true,
      publish := { }
    )
).aggregate(
  core,
  jsoup,
  tagsoup,
  liftjson,
  json4sJackson,
  json4sNative
)

def module(name: String, settings: Seq[Def.Setting[_]] = Seq.empty) =
  Project(name,
    file(name.replace("-", "")),
    settings = Defaults.coreDefaultSettings ++
      Common.settings ++
      Common.testSettings ++
      settings)
    .dependsOn(ufcheck % "test->test")

lazy val core = module("core", xmlDependency)

lazy val liftjson = module("lift-json")
  .dependsOn(core)
  .dependsOn(core % "test->test")

lazy val json4sJackson = module("json4s-jackson")
  .dependsOn(core)
  .dependsOn(core % "test->test")

lazy val json4sNative = module("json4s-native")
  .dependsOn(core)
  .dependsOn(core % "test->test")

lazy val jsoup = module("jsoup")
  .dependsOn(core)
  .dependsOn(core % "test->test")

lazy val tagsoup = module("tagsoup")
  .dependsOn(core)
  .dependsOn(core % "test->test")

lazy val xmlDependency = libraryDependencies ++= Seq("org.scala-lang.modules" %% "scala-xml" % "1.0.6")

/** Util module for using unfiltered with scalacheck */
lazy val ufcheck = project.in(file("ufcheck")).settings(
  scalaVersion := Common.defaultScalaVersion
)

scalacOptions ++= Seq( "-unchecked", "-deprecation" )
