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

import concurrent.ops._
import io.Source
import util.parsing.json._  // TODO: Find better library. This is rather broken.
import jm.oauth._

class TweetStreamBot(server:String, port:Int, chan:String, nick:String,
                     oauth:OAuthCredentials)
        extends Bot(server, port, chan, nick)
{
  override def onConnect {
    val userStreamUrl = "https://userstream.twitter.com/2/user.json"

    val req = new Requester(OAuth.HMAC_SHA1, oauth.consumerSecret, oauth.consumerKey,
                            oauth.token, oauth.tokenSecret, OAuth.VERSION_1)
    val stream = req.getResponse(userStreamUrl, Map()).getEntity.getContent
    val source = Source.fromInputStream(stream, "UTF-8")

    spawn {
      try {
        val iter = source.getLines
        while(!Quit.signaled && iter.hasNext) {
          val line = iter.next
          JSON.parseRaw(line) match {
            case Some(Tweet(t)) => t.sendTweet( sendMessage(chan, _) )
            case _ => println("*** "+line)
          }
        }
      } catch {
        case e:Exception => sendAction(chan, e.getMessage); e.printStackTrace
      }
      Quit.signal
    }
  }

}
