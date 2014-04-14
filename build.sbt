name := "Squirtbot"

version := "0.1"

scalaVersion := "2.10.4"

scalacOptions ++= Seq("-deprecation", "-feature")

fork in run := true

// -Xshare:off to work around http://bugs.java.com/view_bug.do?bug_id=6497639 , http://bugs.java.com/view_bug.do?bug_id=6598065
javaOptions in (run) ++= Seq("-Dcom.sun.management.jmxremote", "-Xmx128M", "-Xshare:off")

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "org.apache.httpcomponents" % "httpclient" % "4.3.2",
  "org.apache.httpcomponents" % "fluent-hc"  % "4.3.2",
  "commons-collections" % "commons-collections" % "3.2.1",
  "io.argonaut" %% "argonaut" % "6.0.2",
  "ch.qos.logback" % "logback-classic" % "1.1.1",
  "org.clapper" % "grizzled-slf4j_2.10" % "1.0.1"
)
