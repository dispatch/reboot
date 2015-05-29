name := "dispatch-lift-json"

description :=
  "Dispatch module providing lift json support"

seq(lsSettings :_*)

(skip in compile) <<= scalaVersion { sv => () => sv == "2.9.3" }

publishArtifact <<= scalaVersion { sv => sv != "2.9.3" }

libraryDependencies ++= Seq(
  "net.liftweb" %% "lift-json" % "2.6" cross CrossVersion.binaryMapped {
    // Makes update resolution happy, but since w'ere not building for 2.9.3
    // we won't end up in runtime version hell by doing this.
    case "2.9.3" => "2.9.1"
    case x => x
  },
  "org.mockito" % "mockito-core" % "1.10.19" % "test"
)
