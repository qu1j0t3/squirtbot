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

class TweetStreamBot(server:String, port:Int, chan:String, nick:String)
        extends Bot(server, port, chan, nick)
{
  override def onConnect {
    import util.parsing.json._  // TODO: Find better library. This is rather broken.
    import jm.oauth._

    val userStreamUrl       = "https://userstream.twitter.com/2/user.json"

    val oauthApiUrl         = "https://api.twitter.com/oauth/request_token"
    val accessTokenUrl      = "https://api.twitter.com/oauth/access_token"
    val authoriseUrl        = "https://api.twitter.com/oauth/authorize"

    val oauthConsumerKey    = "x" // These values are specific to your account; see 
    val oauthConsumerSecret = "y" // https://dev.twitter.com/docs/auth/tokens-devtwittercom

    val oauthToken          = "xx"
    val oauthTokenSecret    = "yy"

    val req = new Requester(OAuth.HMAC_SHA1, oauthConsumerSecret, oauthConsumerKey,
                            oauthToken, oauthTokenSecret, OAuth.VERSION_1)
    val stream = req.getResponse(userStreamUrl, Map()).getEntity.getContent
    val source = Source.fromInputStream(stream, "UTF-8")

    val wrapCol = 60
    val indent = " " * 16

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
                    t.split(' ').foldLeft((0,Nil:List[List[String]],Nil:List[String])) {
                      (s,v) =>
                        val (col,linesAcc,lineAcc) = s
                        val newCol = col + 1 + v.size
                        if(newCol < wrapCol) {  // fits
                          (newCol, linesAcc, v :: lineAcc)
                        } else {  // too long, wrap over
                          (0, lineAcc :: linesAcc, List(v))
                        }
                    } match {
                      case (_,lines,lastLine) =>
                        def sendIndented(line:String) { sendMessage(chan, indent + line) }
                        def sendLines(revLines:List[List[String]]) {
                          val allLines = revLines.reverse.map { _.reverse.mkString(" ") }
                          sendMessage(chan, "@%-13s: %s".format(handle, allLines.head))
                          allLines.tail.foreach(sendIndented)
                        }
                        val allLines = (lastLine :: lines)
                        lastLine match {
                          //case Nil => sendLines( lines )
                          case loneWord :: Nil if !lines.isEmpty =>  // wrap single word back to previous line
                            sendLines( (loneWord :: lines.head) :: lines.tail )
                          case _ =>   sendLines( lastLine :: lines )
                        }
                        shortenUrl(tweetUrl).foreach {
                          line => sendIndented("."*30 + "  " + line)
                        }
                    }
                  }
                case _ => println("JSON object did not have 'text' and 'user' members: "+line)
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
