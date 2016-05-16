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
import annotation.tailrec

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

import org.apache.http.client.config.RequestConfig
import argonaut.Parse
import jm.oauth._

import main.scala.bot1.IrcClientInterface

class TweetStreamBot(oauth: OAuthCredentials, cache: TweetCache) extends Bot {
  val USER_STREAM_JSON = "https://userstream.twitter.com/1.1/user.json"
  val SOCK_TIMEOUT_MS = 5*60*1000
  val STATS_PERIOD_SEC = 60*60*12

  val connectCount = new AtomicInteger(0)
  val tweetCount   = new AtomicInteger(0)
  var tweeters:List[String]   = Nil
  var retweeters:List[String] = Nil
  var hashtags:List[String]   = Nil

  class Transcriber(client:IrcClientInterface, chans:List[String])
          extends Runnable {

    def actionAllChannels(s:String) {
      chans.foreach( client.action(_, s) )
    }

    def processTweetStream {
      val config = RequestConfig.custom
                                .setSocketTimeout(SOCK_TIMEOUT_MS)
                                .setStaleConnectionCheckEnabled(true)
                                .build
      val req = new Requester(OAuth.HMAC_SHA1, oauth.consumerSecret, oauth.consumerKey,
                              oauth.token, oauth.tokenSecret, OAuth.VERSION_1)
      val stream = req.getResponse(USER_STREAM_JSON, Map(), Some(config))
                      .getEntity
                      .getContent

      @tailrec
      def nextLine(iter:Iterator[String]) {
        if(iter.hasNext) {
          val line = iter.next
          val continue = Parse.parseOption(line).map {
            case ParseTweet(t) =>
              tweetCount.incrementAndGet
              synchronized { // blah mutation :(
                if(t.retweetOf.isDefined)
                  retweeters = t.user.screenName :: retweeters
                else
                  tweeters = t.user.screenName :: tweeters
                hashtags = hashtags ++ t.hashtags
              }
              chans.foreach( c =>
                  // has the tweet been seen in the same channel recently?
                  if(!cache.lookupOrPut(c, t)) {
                    t.retweetOf match {
                      case Some(rt) if cache.lookupOrPut(c, rt) =>
                        client.action(c, "@%s retweeted @%s: '%s'"
                                         .format(t.user.screenName, rt.user.screenName, rt.abbreviated))
                      case _ =>
                        client.privmsgGroup(c, t.sendTweet)
                    }
                  } else {
                    client.action(c, "saw that %s too".format(t.description))
                  } )
              true
            case ParseDelete(d) =>
              // FIXME: Ideally announce this only in relevant channel(s)
              cache.getTweetById(d.id).foreach( t =>
                actionAllChannels("@%s deleted '%s'".format(t.user.screenName, t.abbreviated))
              )
              true
            case ParseFavorite(f) =>
              actionAllChannels("@%s favourited '%s'".format(f.source.screenName, f.target.abbreviated))
              true
            case ParseFollow(f) =>
              actionAllChannels("@%s followed @%s".format(f.source.screenName, f.target.screenName))
              true
            case ParseFriends(f) =>
              info("following "+f.userIds.length+" users")
              true
            case ParseDisconnect(d) =>
              actionAllChannels("was disconnected by Twitter (%s, %s, %s)"
                                .format(d.code, d.streamName, d.reason))
              false
            case _ =>
              debug("*** "+line)
              true
          }.getOrElse(true)

          if(continue)
            nextLine(iter)
        }
      }

      connectCount.incrementAndGet
      nextLine(Source.fromInputStream(stream, "UTF-8").getLines)
    }

    override def run {
      @tailrec
      def connect {
        try {
          processTweetStream
        }
        catch {
          case e:InterruptedException =>
            debug(e)
            throw e
          case e:Exception =>
            error(e)
            actionAllChannels("got exception: "+e.getMessage+" ; reconnecting to Twitter...")
        }
        Thread.sleep(10000) // this delay is just plucked out of a hat
        connect // retry forever
      }

      connect
    }
  }

  var thread:Option[Thread] = None  // ewww

  override def onConnect(client:IrcClientInterface, chans:List[String]) {
    val bot = this
    val statsTask = new Runnable() {
      override def run() {
        def top(n:Int, prefix:String, input:List[String]) =
          input.groupBy(_.toLowerCase)
               .map{ case (v,group) => (v,group.length) }
               .toList
               .sortWith( _._2 > _._2 )
               .take(n)
               .map{ case (s,n) => "%s%s (%d)".format(prefix, s, n) }
               .mkString(", ")

        val (ts,rts,hts) = bot.synchronized {
          val get = (tweeters,retweeters,hashtags)
          tweeters = Nil
          retweeters = Nil
          hashtags = Nil
          get
        }
        val clientStats = client.getAndResetStats
        client.notice(chans.head,
                      "Last %d hrs: %d tweets, %d cnx to Twitter, %d msgs sent (~ %.0f%% of max rate), %d throttle notices".format(
                        STATS_PERIOD_SEC/(60*60),
                        tweetCount.getAndSet(0),
                        connectCount.getAndSet(0),
                        clientStats.messageCount,
                        100.0*clientStats.throttleSleepMs/(STATS_PERIOD_SEC*1000.0),
                        clientStats.throttledCount))
        if(!ts.isEmpty)  client.notice(chans.head, "Top tweeters:    " + top(3, "@", ts))
        if(!rts.isEmpty) client.notice(chans.head, "Top re-tweeters: " + top(3, "@", rts))
        if(!hts.isEmpty) client.notice(chans.head, "Top hashtags:    " + top(3, "#", hts))
      }
    }

    val t = new Thread(new Transcriber(client, chans))
    thread = Some(t)
    t.start()

    new ScheduledThreadPoolExecutor(1).scheduleAtFixedRate(
      statsTask, STATS_PERIOD_SEC, STATS_PERIOD_SEC, TimeUnit.SECONDS)
  }

  override def onDisconnect {
    thread.foreach(_.interrupt)
  }

}
