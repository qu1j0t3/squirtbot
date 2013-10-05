/*
    This file is part of "squirtbot", a simple Scala irc bot
    Copyright (C) 2012 Toby Thain, toby@telegraphics.com.au

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package main.scala.squirt

import java.lang.Thread
import java.io.IOException
import org.jibble.pircbot.PircBot
import annotation.tailrec

class Bot(server:String, port:Int, val chan:String, nick:String) extends PircBot {
  protected object Quit extends Signal

  def run {
    //setVerbose(true)
    setEncoding("UTF-8")
    setMessageDelay(300)
    setName(nick)
    connect(server, port)
    joinChannel(chan)
    //sendMessage(chan, "hello.")
    Quit.await // ---------------------
    println("Disconnect")
    disconnect
    dispose
  }

  def checkCommand(message:String) {
    val Command = """^([-\w]+)\W*\s*(.*)$""".r

    message match {
      case Command(nickAddressed, command) if(nickAddressed == nick) =>
        if(command == "quit") Quit.signal
      case _ => ()
    }
  }

  /*override def onPrivateMessage(sender:String, login:String,
                                hostname:String, message:String) {
    checkCommand(message)
  }*/

  override def onMessage(channel:String, sender:String, login:String,
                         hostname:String, message:String) {
    checkCommand(message)
  }

  override def onDisconnect() {
    @tailrec
    def reconn(n:Int) {
      Thread.sleep(5000)
      println("Attempting reconnect (%d)...".format(n))
      ((try {
          reconnect()
          joinChannel(chan)
          None
        }
        catch {
          case e:IOException => println(e.getMessage); Some(true)
          case _:Exception   => Some(false)
        }
      ):Option[Boolean]) match {
        // This is a workaround for the RHS of the catch case not being
        // considered tail position. Also, returning a lambda from the
        // try catch doesn't work either (because it's not a direct self call?)
        case Some(true) => reconn(n+1)
        case Some(false) => Quit.signal
        case None => ()
      }
    }
    reconn(1)
  }

  override def onQuit(sourceNick:String, sourceLogin:String, sourceHostname:String, reason:String) {
    if(sourceNick.equals(nick)) {
      println("QUIT channel: %s".format(reason))
      onDisconnect()
    }
  }

}
