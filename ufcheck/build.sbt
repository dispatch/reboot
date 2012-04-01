libraryDependencies ++= Seq(
  "net.databinder" %% "unfiltered-netty-server" % "0.6.1" % "test",
  "org.slf4j" % "slf4j-simple" % "1.6.4" % "test"
)

testOptions in Test += Tests.Cleanup { loader =>
  val c = loader.loadClass("unfiltered.spec.Cleanup$")
  c.getMethod("cleanup").invoke(c.getField("MODULE$").get(c))
}