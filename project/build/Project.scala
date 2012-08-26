import sbt._

class Project(info: ProjectInfo) extends DefaultProject(info) {
  //val scalatools = "scala-tools" at "http://scala-tools.org/repo-snapshots"

  //val scalatest = "org.scalatest" % "scalatest" % "1.0" % "test->default"
  //val pircbot = "pircbot" % "pircbot" % "1.5" from "http://www.bertails.org/pircbot.jar"

  //val jodatime = "joda-time" % "joda-time" % "1.6"
  override def libraryDependencies = Set(
    // jm.oauth depends on this:
    "org.apache.httpcomponents" % "httpclient" % "4.2.1"
  ) ++ super.libraryDependencies
}
