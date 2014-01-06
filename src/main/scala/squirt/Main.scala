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

import concurrent.ops._
import annotation.tailrec
import util.Random
import math._

import main.scala.bot1.IrcClient

object Main {
  def randomNick = "s-%03d".format(abs(Random.nextInt) % 1000)

  // These values are specific to your account; see
  // https://dev.twitter.com/docs/auth/tokens-devtwittercom
  val oauth = OAuthCredentials("your consumer key",
                               "your consumer secret",
                               "your access token",
                               "your access token secret")

  def main(args:Array[String]) {

  def main(args:Array[String]) {

    // Remember the last 100 tweets so that (one or more) bots don't repeat
    // the same ones in the same channel.
    val shouldCopy: String => Tweet => Boolean = {
      val cache = new LRUMap(100)  // mutable, but at least it's hidden in the closure.
      chan => {
        t => cache.synchronized {
          val key = chan+"/"+t.id
          !(cache.containsKey(key) || { cache.put(key, ()); false })
        }
      }
    }

    @tailrec
    def stayConnected(chans:List[String], nick:String, oauth:OAuthCredentials) {
      import IrcClient._
      try {
        val client = connectSSL(FREENODE, SSL_PORT, "UTF-8")
        val bot = new TweetStreamBot(oauth, shouldCopy)
        bot.run(client, chans, None, nick, "squirtbot", bot.toString)
        client.disconnect
      }
      catch {
        case e:Exception => println(e.getMessage)
      }
      println("reconnecting in 5 secs...")
      Thread.sleep(5000)
      stayConnected(chans, nick, oauth)
    }

    // FreeNode will only use 16 characters of a nick
    spawn { stayConnected(List("#botwar"), "TweetStuff", oauth) }
  }
}
