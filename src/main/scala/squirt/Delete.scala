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

case class Delete(id:String, userId:String)

object ParseDelete {
  def unapply(j:Json):Option[Delete] =
    for {
      delete  <- j -| "delete"
      status  <- delete -| "status"
      idJ     <- status -| "id_str"
      userIdJ <- status -| "user_id_str"
      id      <- idJ.string
      userId  <- userIdJ.string
    } yield Delete(id, userId)
}
