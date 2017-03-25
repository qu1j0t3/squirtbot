name := "Squirtbot"

version := "0.1"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

fork in run := true

// -Xshare:off to work around http://bugs.java.com/view_bug.do?bug_id=6497639 , http://bugs.java.com/view_bug.do?bug_id=6598065
//javaOptions in run ++= Seq("-Dcom.sun.management.jmxremote", "-Xmx128M", "-Xshare:off")

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "org.apache.httpcomponents" % "httpclient" % "4.5.3",
  "org.apache.httpcomponents" % "fluent-hc"  % "4.5.3",
  "commons-collections" % "commons-collections" % "3.2.2",
  "io.argonaut" %% "argonaut" % "6.1",
  "org.scalaz" %% "scalaz-concurrent" % "7.1.10",
  "ch.qos.logback" % "logback-classic" % "1.2.2",
  "org.clapper" %% "grizzled-slf4j" % "1.3.0"
)

mainClass in assembly := Some("squirt.Main")
