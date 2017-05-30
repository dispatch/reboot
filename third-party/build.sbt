name := "third-party"

description :=
  "Third party client library to give easy use of dispatch for common services."

libraryDependencies ++= Seq(
  "net.databinder.dispatch" %% "core" % "0.9.0",
  "org.specs2" %% "specs2" % "1.9" % "test"
)
