name := "dispatch-lift-json"

description :=
  "Dispatch module providing lift json support"

seq(lsSettings :_*)

libraryDependencies <++= scalaVersion( sv =>
  Seq(sv.split("[.-]").toList match {
    case "2" :: "9" :: _ =>
      "net.liftweb" % "lift-json_2.9.1" % "2.4"
    case _ => "net.liftweb" %% "lift-json" % "2.4"
  }, "net.databinder" %% "unfiltered-json" % "0.6.7" % "test")
)
