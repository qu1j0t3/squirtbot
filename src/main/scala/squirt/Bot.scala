/*
    This file is part of "squirtbot", a simple Scala irc bot
    Copyright (C) 2012-2013 Toby Thain, toby@telegraphics.com.au

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

import main.scala.bot1.IrcClientInterface

class Bot {
  def onConnect(client:IrcClientInterface, chan:String, quit:Signal) { }

  def run(client:IrcClientInterface, chan:String, pass:Option[String],
          nick:String, userName:String, realName:String) {
    val quit = new Signal
    //setVerbose(true)
    //setEncoding("UTF-8")
    //setMessageDelay(0)
    client.register(pass, nick, userName, realName)
    client.join(chan)
    //sendMessage(chan, "hello.")
    onConnect(client, chan, quit)
    client.run {
      msg => true
    }
    quit.signal
    println("Disconnect")
    client.disconnect
  }
}

