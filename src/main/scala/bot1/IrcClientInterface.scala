package main.scala.bot1

// Operations that can be performed on an IRC client socket
// (typically connected)

trait IrcClientInterface {
  def run(handle: IrcMessage => Boolean):Unit
  def command(cmd:String, middle:Seq[String], trailing:Option[String]):Unit
  def register(pass:Option[String], nick:String, user:String, realName:String):Unit
  def join(chan:String):Unit
  def part(chan:String, msg:Option[String]):Unit
  def quit(msg:Option[String]):Unit
  def privmsg(target:String, msg:String):Unit
  def disconnect:Unit
}