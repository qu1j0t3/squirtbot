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

class Tweet(val text:String,
            val id:String,
            val user:TwitterUser,
            val retweet:Option[Tweet]) {
  def url:String = "http://twitter.com/" + user.screenName + "/status/" + id
}

object Tweet {
  // Note: the parameter type is a more specific type than need
  //       be matched in the case which refers to this extractor.
  //       If the type doesn't match, match fails before extractor is tried.
  //       (see http://www.artima.com/pins1ed/extractors.html )
  // Cannot match on Option[T] due to type erasure.
  def unapply(m:JSONObject):Option[Tweet] =
    (m.obj.get("text"),
     m.obj.get("id_str"),
     m.obj.get("user"),
     m.obj.get("retweeted_status")) match {
      case (Some(text:String),
            Some(id:String),
            Some(TwitterUser(user)),
            maybeRT) =>
        Some(new Tweet(text,
                       id,
                       user,
                       maybeRT match {
                         case Some(rt:JSONObject) => unapply(rt)
                         case _ => None
                       } ))
      case _ =>
        None
    }
}
