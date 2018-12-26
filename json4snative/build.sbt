name := "dispatch-json4s-native"

description :=
  "Dispatch module providing json4s native support"

libraryDependencies ++= Seq(
  "org.json4s"    %% "json4s-core"              % Common.json4sVersion,
  "org.json4s"    %% "json4s-native"            % Common.json4sVersion,
  "ws.unfiltered" %% "unfiltered-netty-server"  % Common.unfilteredNettyVersion % Test
)
