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

package squirt

import argonaut.Argonaut._
import argonaut._

final case class TwitterUrl(url: String, expandedUrl: String, startIndex: Int, endIndex: Int)

object TwitterUrl {
  implicit val decode: DecodeJson[TwitterUrl] =
    jdecode3L( (indices:List[Int], url:String, expandedUrl:String) =>
      indices match {
        case List(start, end) => TwitterUrl(url, expandedUrl, start, end)
        // ignore match failure
      }
    )("indices", "url", "expanded_url")
}
