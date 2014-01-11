/*
    This file is part of "squirtbot", a simple Scala irc bot
    Copyright (C) 2012-2013 Toby Thain, toby@telegraphics.com.au

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
import io.Source
import argonaut.Parse
import jm.oauth._
import annotation.tailrec

import main.scala.bot1.IrcClientInterface

class TweetStreamBot(oauth: OAuthCredentials, cache: TweetCache)
        extends Bot {
  val userStreamUrl = "https://userstream.twitter.com/2/user.json"

  override def onConnect(client:IrcClientInterface, chans:List[String], quit:Signal) {
    spawn {
      val req = new Requester(OAuth.HMAC_SHA1, oauth.consumerSecret, oauth.consumerKey,
                              oauth.token, oauth.tokenSecret, OAuth.VERSION_1)
      val stream = req.getResponse(userStreamUrl, Map()).getEntity.getContent

      @tailrec
      def nextLine(iter:Iterator[String]) {
        if(!quit.signaled && iter.hasNext) {
          val line = iter.next
          val continue = Parse.parseOption(line).map {
            case ParseTweet(t) =>
              // synchronize on the channel name, so that multiple bots
              // configured in this program won't interrupt each other.
              // careful: will only work as expected if chan is an
              // intern'd String (e.g. a literal); see http://stackoverflow.com/a/9698305
              chans.foreach( c =>
                c.synchronized {
                  // has the tweet been seen in the same channel recently?
                  if(!cache.lookupOrPut(c, t)) {
                    t.retweetOf match {
                      case Some(rt) if cache.lookupOrPut(c, rt) =>
                        client.action(c, "@%s retweeted @%s: '%s'"
                                         .format(t.user.screenName, rt.user.screenName, rt.abbreviated))
                      case _ =>
                        t.sendTweet( client.privmsg(c, _) )
                    }
                  } else {
                    client.action(c, "saw that %s too".format(t.description))
                  }
                } )
              true
            case ParseDelete(d) =>
              chans.foreach( c =>
                cache.getTweetById(d.id).foreach( t =>
                  c.synchronized {
                    client.action(c, "@%s deleted '%s'".format(t.user.screenName, t.abbreviated))
                  } ) )
              true
            case ParseFavorite(f) =>
              chans.foreach( c =>
                c.synchronized {
                  client.action(c, "@%s favourited '%s'".format(f.source.screenName, f.target.abbreviated))
                } )
              true
            case ParseDisconnect(d) =>
              println("whoa, dude. Twitter disconnected us (%s, %s, %s)".format(d.code, d.streamName, d.reason))
              false
            case _ =>
              println("*** "+line)
              true
          }.getOrElse(true)

          if(continue)
            nextLine(iter)
        }
      }

      try {
        nextLine(Source.fromInputStream(stream, "UTF-8").getLines)
      }
      catch {
        case e:Exception =>
          chans.foreach(client.action(_, "saw an exception: "+e.getMessage))
          e.printStackTrace
      }

      quit.signal
    }
  }

}
