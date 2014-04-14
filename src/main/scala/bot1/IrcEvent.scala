package main.scala.bot1

sealed abstract class IrcEvent
case class IrcPrivMsg(target:String, message:String) extends IrcEvent
case class IrcAction(target:String, action:String) extends IrcEvent
