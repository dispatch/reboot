libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck"              % Common.scalacheckVersion      % Test,
  "ws.unfiltered"  %% "unfiltered-netty-server" % Common.unfilteredNettyVersion % Test excludeAll ExclusionRule(organization = "io.netty"),
  "org.slf4j"      %  "slf4j-simple"            % Common.slf4jVersion           % Test
)
