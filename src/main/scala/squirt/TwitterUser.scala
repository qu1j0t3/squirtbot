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

class TwitterUser(val screenName:String, val name:String)

object TwitterUser {
  def unapply(u:JSONObject):Option[TwitterUser] =
    (u.obj.get("screen_name"),u.obj.get("name")) match {
      case (Some(screenName:String),Some(name:String)) =>
        Some(new TwitterUser(screenName, name))
      case _ => None
    }
}
