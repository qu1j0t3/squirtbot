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
    "org.apache.httpcomponents" % "httpclient" % "4.3.1",
    "org.apache.httpcomponents" % "fluent-hc"  % "4.3.1"
  ) ++ super.libraryDependencies
}
