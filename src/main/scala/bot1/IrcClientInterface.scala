package main.scala.bot1

// Operations that can be performed on an IRC client socket
// (typically connected)

trait IrcClientInterface {
  def run(handle: IrcMessage => Boolean)
  def command(cmd:String, middle:Seq[String], trailing:Option[String])
  def register(pass:Option[String], nick:String, user:String, realName:String)
  def join(chan:String)
  def part(chan:String, msg:Option[String])
  def quit(msg:Option[String])
  def privmsg(target:String, msg:String)
  def notice(target:String, msg:String)
  def privmsgGroup(target:String, group:Seq[String])
  def action(target:String, action:String)
  def disconnect

  case class Stats(messageCount:Int, throttledCount:Int)
  def getAndResetStats:Stats
}