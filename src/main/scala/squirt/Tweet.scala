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

import util.parsing.json._
import org.jibble.pircbot.Colors._

class Tweet(val text:String,
            val id:String,
            val user:TwitterUser)
{
  def url:String = "http://twitter.com/" + user.screenName + "/status/" + id

  val indentCols = 20
  val wrapCols   = 60

  val ScreenName = """@.*""".r
  val HashTag    = """#.*""".r
  val Url        = """https?:\/\/.*""".r

  def highlightWord(word:String) = word match {
    case ScreenName() => MAGENTA + word + NORMAL
    case HashTag()    => CYAN    + word + NORMAL
    case Url()        => PURPLE  + word + NORMAL
    case _            => word
  }
  def highlightUrl(s:String)     = DARK_GREEN + s + NORMAL
  def highlightNick(s:String)    = BOLD       + s + NORMAL
  def highlightLeftCol(s:String) = DARK_BLUE  + s + NORMAL

  def format(send:String=>Unit, leftColumn:List[String], text:String) {
    val wrapped = text
                  .split('\n')          // respect newlines in original tweet
                  .flatMap { WordWrap.wrap(_, wrapCols, highlightWord) }
    leftColumn.zipAll(wrapped, "", "").zipWithIndex.foreach {
      case ((a,b),i) =>
        send((if(i == 0) highlightNick(a) else highlightLeftCol(a)) +
             " "*(2 max (indentCols - a.size)) + b)
    }
    send(highlightUrl("."*40 + "  " + Ur1Ca.shortenUrl(url).getOrElse(url)))
  }

  def sendTweet(send:String=>Unit) {
    format(send, List("@"+user.screenName), text)
  }
}

object Tweet {
  // Note: Parameter type JSONObject is already narrowing the match.
  //       (see http://www.artima.com/pins1ed/extractors.html )
  def unapply(m:JSONObject):Option[Tweet] =
    (m.obj.get("text"),
     m.obj.get("id_str"),
     m.obj.get("user"),
     m.obj.get("retweeted_status")) match {
      case (Some(text:String),
            Some(id:String),
            Some(TwitterUser(user)),
            retweetedStatus) =>
        Some(
          retweetedStatus match {
            case Some(Tweet(rt)) => new Retweet(text, id, user, rt)
            case _               => new Tweet(text, id, user)
          } )
      case _ =>
        None  // don't recognise this as a tweet
    }
}
