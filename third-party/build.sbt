name := "third-party"

description :=
  "Third party client library to give easy use of dispatch for common services."

libraryDependencies ++= Seq(
  "com.ning" % "async-http-client" % "1.7.5",
  "net.databinder.dispatch" %% "core" % "0.9.0"
)
