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

import argonaut._
import org.jibble.pircbot.Colors._

case class Tweet(text:String, id:String, user:TwitterUser, retweetOf:Option[Tweet]) {
  def url:String = "http://twitter.com/" + user.screenName + "/status/" + id

  val indentCols = 17
  val wrapCols   = 47

  val ScreenName = """@.+""".r
  val HashTag    = """#.+""".r
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
  
  def description:String =
    retweetOf.map(rt => "retweet of @%s by @%s".format(rt.user.screenName, user.screenName))
             .getOrElse("tweet by @"+user.screenName)

  def descriptionList:List[String] =
    retweetOf.map(rt => List("@"+rt.user.screenName,
                             " retweeted by",
                             " @"+user.screenName))
             .getOrElse(List("@"+user.screenName))

  def abbreviated = {
    val abbrev = text.split(' ').take(8).mkString(" ")
    if(abbrev != text) abbrev+"..." else abbrev
  }

  def sendTweet(send:String=>Unit) {
    // If it's a retweet, send full text of original tweet,
    // otherwise full text of this tweet
    format(send, descriptionList, retweetOf.map(_.text).getOrElse(text))
  }
}

object ParseTweet {
  def unapply(j:Json):Option[Tweet] =
    for {
      textJ <- j -| "text"
      idJ   <- j -| "id_str"
      userJ <- j -| "user"
      text  <- textJ.string
      id    <- idJ.string
      user  <- ParseTwitterUser.unapply(userJ)
    }
    yield (j -| "retweeted_status") match {
      case Some(ParseTweet(rt)) => Tweet(text, id, user, Some(rt))
      case _                    => Tweet(text, id, user, None)
    }
}
