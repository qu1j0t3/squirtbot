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

import io.Source
import java.lang.Thread
import org.jibble.pircbot.PircBot

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.fluent._

class Bot(server:String, port:Int, val chan:String, nick:String) extends PircBot {
  protected object Quit extends Signal

  def run {
    //setVerbose(true)
    setName(nick)
    connect(server, port)
    joinChannel(chan)
    //sendMessage(chan, "hello.")
    Quit.await // ---------------------
    println("Disconnect")
    disconnect
    dispose
  }

  override def onMessage(channel:String, sender:String, login:String,
                         hostname:String, message:String) {
    val Command = """^([-\w]+)\W*\s*(.*)$""".r

    message match {
      case Command(nickAddressed, command) if(nickAddressed == nick) =>
        if(command == "quit") Quit.signal
      case _ => ()
    }
  }

  protected val client = new DefaultHttpClient  // TODO: set long keepalive (1 minute?)
  
  def shortenUrl(longUrl:String):Option[String] = {
    val ShortUrl = """^.*Your ur1 is: <a href="(.*?)">.*$""".r

    val resp = Request.Post("http://ur1.ca/")
                      .bodyForm(Form.form().add("longurl", longUrl).build())
                      .execute()
                      .returnResponse()
    val source = Source.fromInputStream(resp.getEntity.getContent, "UTF-8")
    source.getLines.collectFirst { case ShortUrl(url) => url }
  }
}
