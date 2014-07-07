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

import WordWrap._

case class Tweet(text:String, id:String, user:TwitterUser,
                 hashtags:List[String], urls:List[TwitterUrl],
                 retweetOf:Option[Tweet]) {
  def url:String = "http://twitter.com/" + user.screenName + "/status/" + id

  val indentCols = 17
  val wrapCols   = 47

  def highlight(word:Word) = word match {
    case UserMentionWord(s) => MAGENTA + s + NORMAL
    case HashTagWord(s)     => CYAN    + s + NORMAL
    case UrlWord(s)         => PURPLE  + s + NORMAL
    case PlainWord(s)       => s
  }
  def highlightUrl(s:String)     = DARK_GREEN + s + NORMAL
  def highlightNick(s:String)    = BOLD       + s + NORMAL
  def highlightLeftCol(s:String) = DARK_BLUE  + s + NORMAL

  def fixEntities(s:String) =
    s.replace("&lt;",  "<")
     .replace("&gt;",  ">")
     .replace("&amp;", "&")

  def splitWithIndex(s:String, c:Char):List[(Int,String)] =
    s.split(c).foldLeft( (0,Nil:List[(Int,String)]) )(
      (acc, line) =>
        acc match { case (col,lines) => (col + line.size + 1, (col,line) :: lines) }
    )._2.reverse

  def format(leftColumn:List[String], text:String):List[String] = {
    // 1) convert string into lines. keep starting column for each line
    // 2) break lines into words. classify each word. match each word
    //    to URL entity indexes; if matched, add parenthesised hostname
    // 3) stringify each word with colour codes, then flatMap

    // respect newlines in original tweet
    val wrapped:List[String] =
      splitWithIndex(text, '\n').flatMap {
        case (lineIdx,line) => {
          val words:List[Word] = splitWithIndex(line, ' ').flatMap {
            case (wordIdx,word) => // could possibly substitute the t.co url with expanded if it was shorter
              classify(fixEntities(word)) ::
                urls.filter( u => ((c:Int) => c > 0 && c <= word.size)(u.endIndex - (lineIdx + wordIdx)) )
                    .map( u => UrlWord("(" + UrlResolver.resolve(u.expandedUrl).getOrElse("error") + ")") )
          }
          wrap(words, wrapCols).map( _.map(highlight(_)) )
        }
      }.map(_.mkString(" "))

    leftColumn.zipAll(wrapped, "", "").zipWithIndex.map {
      case ((a,b),i) =>
        (if(i == 0) highlightNick(a) else highlightLeftCol(a)) +
             " "*(2 max (indentCols - a.size)) + b
    } ++ List(highlightUrl("."*40 + "  " + Ur1Ca.shortenUrl(url).getOrElse(url)))
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
    val abbrev = text.split("\\s").take(8).mkString(" ")
    if(abbrev != text) abbrev+"..." else abbrev
  }

  // If it's a retweet, send full text of original tweet,
  // otherwise full text of this tweet
  def sendTweet:List[String] =
    format(descriptionList, retweetOf.map(_.text).getOrElse(text))
}

object ParseTweet {
  def getHashtags(hashtags:List[Json]):List[String] =
    for {
      ht    <- hashtags
      textJ <- ht -| "text"
      text  <- textJ.string
    } yield text

  def getUrls(urls:List[Json]):List[TwitterUrl] =
    urls.flatMap(ParseTwitterUrl.unapply(_))

  def unapply(j:Json):Option[Tweet] =
    for {
      textJ <- j -| "text"
      idJ   <- j -| "id_str"
      userJ <- j -| "user"
      text  <- textJ.string
      id    <- idJ.string
      user  <- ParseTwitterUser.unapply(userJ)
      entitiesJ <- j -| "entities"
      hashtagsJ <- entitiesJ -| "hashtags"
      hashtags  <- hashtagsJ.array
      urlsJ <- entitiesJ -| "urls"
      urls  <- urlsJ.array
    }
    yield (j -| "retweeted_status") match {
      case Some(ParseTweet(rt)) =>
        Tweet(text, id, user, getHashtags(hashtags), rt.urls, Some(rt))
      case _ =>
        Tweet(text, id, user, getHashtags(hashtags), getUrls(urls), None)
    }
}
