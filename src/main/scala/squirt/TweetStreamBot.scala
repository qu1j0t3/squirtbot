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
import annotation.tailrec

class TweetStreamBot(server:String, port:Int, chan:String, nick:String,
                     oauth:OAuthCredentials)
        extends Bot(server, port, chan, nick)
{
  override def onConnect {
    import util.parsing.json._  // TODO: Find better library. This is rather broken.
    import jm.oauth._

    val userStreamUrl       = "https://userstream.twitter.com/2/user.json"

    val req = new Requester(OAuth.HMAC_SHA1, oauth.consumerSecret, oauth.consumerKey,
                            oauth.token, oauth.tokenSecret, OAuth.VERSION_1)
    val stream = req.getResponse(userStreamUrl, Map()).getEntity.getContent
    val source = Source.fromInputStream(stream, "UTF-8")

    def sendIndented(line:String) { sendMessage(chan, " "*16 + line) }

    def wordWrap(text:String, wrapCol:Int):List[String] = {
      val (_,lines,lastLine) =
        text.split(' ').foldLeft((0,Nil:List[List[String]],Nil:List[String])) {
          (state, word) =>
            val (col,linesAcc,lineAcc) = state
            val newCol = col + 1 + word.size
            if(col == 0) {  // always take first word
              (word.size, linesAcc, word :: lineAcc)
            } else if (newCol <= wrapCol) {  // word fits on line
              (newCol, linesAcc, word :: lineAcc)
            } else {  // too long, wrap word to next line
              (0, lineAcc :: linesAcc, List(word))
            }
        }
      (lastLine :: lines).reverse.map { _.reverse.mkString(" ") }
    }

    spawn {
      try {
        val iter = source.getLines
        while(!Quit.signaled && iter.hasNext) {
          val line = iter.next
          JSON.parseRaw(line) match { // Nasty, because of the poor typing in util.parsing.json
            case Some(JSONObject(m)) =>
              (m.get("text"), m.get("id_str"), m.get("user")) match {
                case (Some(t:String), Some(id:String), Some(JSONObject(u))) =>
                  for(handle <- u.get("screen_name")) {
                    val tweetUrl = "http://twitter.com/"+handle+"/status/"+id
                    /* un-word-wrapped:
                    sendMessage(chan, "@"+handle+": "+t+
                                      shortenUrl(tweetUrl).map{ " | " + _ }.getOrElse(""))
                    */
                    val lines = wordWrap(t, 60)
                    sendMessage(chan, "@%-13s: %s".format(handle, lines.head))
                    lines.tail.foreach(sendIndented)
                    sendMessage(chan, "."*30 + "  " + shortenUrl(tweetUrl).getOrElse(tweetUrl))
                  }
                case _ => println("Not tweet: "+line)
              }
            case _ => println("*** "+line)
          }
        }
      } catch {
        case e => sendAction(chan, e.getMessage); e.printStackTrace
      }
      Quit.signal
    }
  }

}
