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

case class TwitterUrl(url:String, expandedUrl:String, startIndex:Int, endIndex:Int)

object ParseTwitterUrl {
  def unapply(j:Json):Option[TwitterUrl] = {
    // This parser syntax proposed by @tixxit
    val c = j.acursor
    ( for {
        url         <- c.downField("url").as[String]
        expandedUrl <- c.downField("expanded_url").as[String]
        indices      = c.downField("indices")
        start       <- indices.downN(0).as[Int]
        end         <- indices.downN(1).as[Int]
      } yield TwitterUrl(url, expandedUrl, start, end)
    ).toOption
  }
}
