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

case class Delete(id:String, userId:String)

object ParseDelete {
  def unapply(m:JSONObject):Option[Delete] =
    m.obj.get("delete") match {
      case Some(d:JSONObject) =>
        d.obj.get("status") match {
          case Some(s:JSONObject) =>
            (s.obj.get("id_str"), s.obj.get("user_id_str")) match {
              case (Some(id:String),Some(userId:String)) =>
                Some(Delete(id, userId))
              case _ => None
            }
          case _ => None
        }
      case _ => None
    }
}
