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

// FIXME: These should be optional fields; we shouldn't miss a Disconnect
//        just because any are missing.
case class Disconnect(code:String, streamName:String, reason:String)

object ParseDisconnect {
  def unapply(j:Json):Option[Disconnect] =
    for {
      disconnect  <- j -| "disconnect"
      codeJ       <- disconnect -| "code"
      streamNameJ <- disconnect -| "stream_name"
      reasonJ     <- disconnect -| "reason"
      code        <- codeJ.string
      streamName  <- streamNameJ.string
      reason      <- reasonJ.string
    } yield Disconnect(code, streamName, reason)
}
