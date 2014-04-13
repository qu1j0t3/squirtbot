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

import main.scala.bot1.IrcClient._

import grizzled.slf4j.Logging

object Main extends Logging {
  def randomNick = "s-%03d".format(abs(Random.nextInt) % 10000)

  // These values are specific to your account; see
  // https://dev.twitter.com/docs/auth/tokens-devtwittercom
  val oauth = OAuthCredentials("your consumer key",
                               "your consumer secret",
                               "your access token",
                               "your access token secret")

  def main(args:Array[String]) {
    val cache = new TweetCache
    val CONNECT_BACKOFF_MS = 10000

    @tailrec
    def stayConnected(chans:List[String], nick:String, oauth:OAuthCredentials, backoffMs:Int) {
      val newBackoff =
        try {
          // The client is the interface to the IRC service.
          // It includes a concurrent thread that manages rate limited
          // sending of messages to the server.
          val client = connectSSL(FREENODE, SSL_PORT, "UTF-8")
          val bot = new TweetStreamBot(oauth, cache)
          try {
            // A bot defines its behaviour through the onConnect,
            // onDisconnect, and onCommand methods. It runs until
            // disconnected from the server for any reason.
            val bot = new TweetStreamBot(oauth, cache)
            bot.run(client, chans, None, nick, "squirtbot", bot.toString)
            CONNECT_BACKOFF_MS
          }
          finally {
            client.disconnect
          }
        }
        catch {
          case e:Exception =>
            error(e)
            backoffMs
        }
      info("reconnecting in %.1f secs...".format(newBackoff/1000.))
      Thread.sleep(newBackoff)
      stayConnected(chans, nick, oauth, newBackoff + newBackoff/2)
    }

    // FreeNode will only use 16 characters of a nick
    spawn { stayConnected(List("#botwar"), "TweetStuff", oauth) }
  }
}
