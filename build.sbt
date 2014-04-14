name := "Squirtbot"

version := "0.1"

scalaVersion := "2.10.4"

scalacOptions += "-deprecation"

fork in run := true

javaOptions in (run) += "-Dcom.sun.management.jmxremote"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "org.apache.httpcomponents" % "httpclient" % "4.3.2",
  "org.apache.httpcomponents" % "fluent-hc"  % "4.3.2",
  "commons-collections" % "commons-collections" % "3.2.1",
  "io.argonaut" %% "argonaut" % "6.0.2",
  "ch.qos.logback" % "logback-classic" % "1.1.1",
  "org.clapper" % "grizzled-slf4j_2.10" % "1.0.1"
)
