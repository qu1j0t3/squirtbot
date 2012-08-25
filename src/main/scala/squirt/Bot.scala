package main.scala.squirt

import java.lang.Thread
import org.jibble.pircbot.PircBot

class Bot(server:String, port:Int, chan:String, nick:String) extends PircBot {
  private object Quit extends Signal

  def run {
    //setVerbose(true)
    setName(nick)
    connect(server, port)
    joinChannel(chan)
    sendMessage(chan, "hello.")
    Quit.await // ---------------------
    println("Disconnect")
    disconnect
    dispose
  }

  override def onMessage(channel:String, sender:String, login:String,
                         hostname:String, message:String) {
    val Command = """^([-\w]+)\W*\s*(.*)$""".r

    println(">> "+message)
    message match {
      case Command(nickAddressed, command) if(nickAddressed == nick) =>
        println("recognised command: "+command)
        if(command == "quit") Quit.signal
      case _ => ()
    }
  }
}
