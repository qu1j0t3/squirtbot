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

case class TwitterUser(id:String, screenName:String, name:String)

object ParseTwitterUser {
  val codec: CodecJson[TwitterUser] =
    CodecJson.casecodec3(TwitterUser.apply, TwitterUser.unapply)("id_str", "screen_name", "name")

  implicit val decoder = codec.Decoder

  def unapply(j:Json):Option[TwitterUser] = j.as[TwitterUser].toOption
}
