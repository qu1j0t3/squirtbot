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

import java.lang.Thread
import concurrent.ops._
import util.Random
import math._

object Main {
  def randomNick = "s-%03d".format(abs(Random.nextInt) % 1000)

  def main(args:Array[String]) {
    // All freenode servers listen on ports 6665, 6666, 6667,
    // 6697 (SSL only), 7000 (SSL only), 7070 (SSL only), 8000, 8001 and 8002.
    val bot = new Bot("irc.freenode.net", 6667, "#botwar", randomNick) {
      override def onConnect {
        spawn {
          var i = 1
          while(!Quit.signaled) {
            sendAction(chan, i.toString)
            i += 1
            Thread.sleep(2000)
          }
        }
      }
    }

    bot.run
  }
}