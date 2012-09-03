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

    val indentCols = 20
    val wrapCols   = 60
    val errorStr   = Colors.RED + "error" + Colors.NORMAL

    val ScreenName = """@.*""".r
    val HashTag    = """#.*""".r
    val Url        = """https?:\/\/.*""".r

    def colourNick(s:String) = Colors.BOLD + "@" + s + Colors.NORMAL

    def highlightWord(word:String) = word match {
      case ScreenName() => Colors.MAGENTA + word + Colors.NORMAL
      case HashTag()    => Colors.CYAN    + word + Colors.NORMAL
      case Url()        => Colors.PURPLE  + word + Colors.NORMAL
      case _            => word
    }

    def fixEntities(s:String) =
      s.replaceAll("&lt;",  "<")
       .replaceAll("&gt;",  ">")
       .replaceAll("&amp;", "&")

    def wordWrap(text:String, wrapCol:Int):List[String] = {
      val (_,lines,lastLine) =
        text.split(' ')
        .map(fixEntities)
        .foldLeft((0,Nil:List[List[String]],Nil:List[String])) {
          (state,word) =>
            if(word == "") {
              state
            } else {
              val (col,linesAcc,lineAcc) = state
              val newCol = col + 1 + word.size
              val colourWord = highlightWord(word)
              if(col == 0) {                   // always take first word
                (word.size, linesAcc, colourWord :: lineAcc)
              } else if (newCol <= wrapCol) {  // word fits on line
                (newCol, linesAcc, colourWord :: lineAcc)
              } else {                         // too long, wrap to next line
                (0, lineAcc :: linesAcc, List(colourWord))
              }
            }
        }
      (lastLine :: lines).reverse.map { _.reverse.mkString(" ") }
    }

    def showTweet(leftColumn:List[String], text:String, tweetUrl:String) {
      val wrapped = text
                    .replaceAll("、", "、 ")  // hack to allow Japanese to wrap better
                    .split('\n')             // respect newlines in original tweet
                    .flatMap { wordWrap(_, wrapCols) }
      leftColumn.zipAll(wrapped, "", "").zipWithIndex.foreach {
        case ((a,b),i) =>
          sendMessage(chan, (if(i == 0) Colors.BOLD else Colors.DARK_BLUE) +
                            a + Colors.NORMAL +
                            " "*(2 max (indentCols - a.size)) + b)
      }
      sendMessage(chan, Colors.DARK_GREEN + "."*40 + "  " +
                        shortenUrl(tweetUrl).getOrElse(tweetUrl) +
                        Colors.NORMAL)
    }

    spawn {
      try {
        val iter = source.getLines
        while(!Quit.signaled && iter.hasNext) {
          val line = iter.next
          JSON.parseRaw(line) match { // Nasty, because of the poor typing in util.parsing.json
            case Some(JSONObject(m)) =>
              (m.get("text"), m.get("id_str"), m.get("user")) match {
                case (Some(tweet:String),      // required
                      Some(statusId:String),   // required
                      Some(JSONObject(u))) =>  // required
                  u.get("screen_name") match {
                    case Some(screenName:String) => {
                      val tweetUrl = "http://twitter.com/"+screenName+"/status/"+statusId
                      m.get("retweeted_status") match {
                        case Some(JSONObject(rtStatus)) => // It is a re-tweet
                          (rtStatus.get("text"), rtStatus.get("user")) match {
                            case (Some(t:String),
                                  Some(JSONObject(u))) =>
                              showTweet(List(u.get("screen_name").map{ "@"+_ }.getOrElse(errorStr),
                                             " retweeted by",
                                             " @"+screenName),
                                        t,
                                        tweetUrl)
                            case _ => println("Could not match retweeted_status object in "+line)
                          }
                        case None => // Not a re-tweet. TODO: Replies
                          showTweet(List("@"+screenName), tweet, tweetUrl)
                      }
                    }
                    case _ => println("Could not match user screen_name in "+line)
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
