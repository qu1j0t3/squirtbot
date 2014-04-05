import sbt._

class Project(info: ProjectInfo) extends DefaultProject(info) with assembly.AssemblyBuilder {
  override def compileOptions:Seq[CompileOption] = List( /*Verbose,*/ Unchecked, Deprecation)

  override def packageOptions = Seq(MainClass("main.scala.squirt.Main"))

  //val scalatools = "scala-tools" at "http://scala-tools.org/repo-snapshots"

  //val scalatest = "org.scalatest" % "scalatest" % "1.0" % "test->default"
  //val pircbot = "pircbot" % "pircbot" % "1.5" from "http://www.bertails.org/pircbot.jar"

  //val jodatime = "joda-time" % "joda-time" % "1.6"
  override def libraryDependencies = Set(
    // jm.oauth depends on this:
    "org.apache.httpcomponents" % "httpclient" % "4.3.2",
    "org.apache.httpcomponents" % "fluent-hc"  % "4.3.2",
    "commons-collections" % "commons-collections" % "3.2.1",
    "io.argonaut" %% "argonaut" % "6.0.2",
    "ch.qos.logback" % "logback-classic" % "1.1.1",
    "org.clapper" % "grizzled-slf4j_2.9.2" % "0.6.10"
  ) ++ super.libraryDependencies
}
