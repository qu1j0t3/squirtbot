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
import collection.mutable.HashMap
import org.jibble.pircbot.Colors

class TweetStreamBot(server:String, port:Int, chan:String, nick:String,
                     oauth:OAuthCredentials)
        extends Bot(server, port, chan, nick)
{
  override def onConnect {
    import util.parsing.json._  // TODO: Find better library. This is rather broken.
    import jm.oauth._

    val userStreamUrl = "https://userstream.twitter.com/2/user.json"

    val req = new Requester(OAuth.HMAC_SHA1, oauth.consumerSecret, oauth.consumerKey,
                            oauth.token, oauth.tokenSecret, OAuth.VERSION_1)
    val stream = req.getResponse(userStreamUrl, Map()).getEntity.getContent
    val source = Source.fromInputStream(stream, "UTF-8")

    def colourNick(s:String) = Colors.BOLD + "@" + s + Colors.NORMAL

    def fixEntities(s:String) =
      s.replaceAll("&lt;",  "<")
       .replaceAll("&gt;",  ">")
       .replaceAll("&amp;", "&")

    val indentCols = 20
    val wrapCols = 60

    def wordWrap(text:String, wrapCol:Int):List[String] = {
      val (_,lines,lastLine) =
        text.split(' ').foldLeft((0,Nil:List[List[String]],Nil:List[String])) {
          (state, rawWord) =>
            if(rawWord == "") {
              state
            } else {
              val word = fixEntities(rawWord)
              val (col,linesAcc,lineAcc) = state
              val newCol = col + 1 + word.size
              val colourWord = word(0) match {
                case '@' => Colors.MAGENTA + word + Colors.NORMAL
                case '#' => Colors.CYAN    + word + Colors.NORMAL
                case _   => word
              }
              if(col == 0) {  // always take first word
                (word.size, linesAcc, colourWord :: lineAcc)
              } else if (newCol <= wrapCol) {  // word fits on line
                (newCol, linesAcc, colourWord :: lineAcc)
              } else {  // too long, wrap word to next line
                (0, lineAcc :: linesAcc, List(colourWord))
              }
            }
        }
      (lastLine :: lines).reverse.map { _.reverse.mkString(" ") }
    }

    spawn {
      try {
        val iter = source.getLines
        while(!Quit.signaled && iter.hasNext) {
          val line = iter.next
          //println(line)
          JSON.parseRaw(line) match { // Nasty, because of the poor typing in util.parsing.json
            case Some(JSONObject(m)) if m.isDefinedAt("text") =>
              (m.get("id_str"), m.get("user"), m.get("retweeted_status")) match {
                case (Some(statusId:String),
                      Some(JSONObject(u)),
                      rt:Option[JSONObject]) =>
                  (u.get("screen_name"), u.get("id_str")) match {
                    case (Some(screenName:String), Some(userId:String)) => {
                      val tweetUrl = "http://twitter.com/"+screenName+"/status/"+statusId
                      /* un-word-wrapped:
                      sendMessage(chan, "@"+screenName+": "+t+
                                        shortenUrl(tweetUrl).map{ " | " + _ }.getOrElse(""))
                      */
                      val (lhs:List[String],text:String) = rt match {
                        case Some(rtStatus) => // It is a re-tweet
                          (rtStatus.obj.get("text"), rtStatus.obj.get("user")) match {
                            case (Some(t:String), Some(JSONObject(u))) =>
                              u.get("screen_name") match {
                                case Some(origScreenName:String) =>
                                  (List("@"+origScreenName, " retweeted by", " @"+screenName), t)
                              }
                            case _ => println("Could not match retweeted_status object in "+line)
                          }
                        case None => // Not a re-tweet. TODO: Replies
                          m.get("text") match {
                            case Some(t:String) => (List("@"+screenName), t)
                          }
                      }
                      val wrapped = text.split('\n').flatMap { wordWrap(_, wrapCols) }
                      lhs.zipAll(wrapped, "", "").zipWithIndex.foreach {
                        case ((a,b),i) =>
                          sendMessage(chan, (if(i == 0) Colors.BOLD else Colors.DARK_BLUE) +
                                            a + Colors.NORMAL +
                                            " "*(2 max (indentCols - a.size)) + b)
                      }
                      sendMessage(chan, Colors.DARK_GREEN + "."*40 + "  " +
                                        shortenUrl(tweetUrl).getOrElse(tweetUrl) +
                                        Colors.NORMAL)
                    }
                    case _ => println("Could not match user object in "+line)
                  }
                case _ => println("Could not match tweet: "+line)
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
