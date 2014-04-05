package main.scala.bot1

import util.Random
import math._

object Main {
  val PLAINTEXT_PORT = 6667
  val SSL_PORT = 6697

  def randomNick = "s-%03d".format(abs(Random.nextInt) % 1000)

  def main(args:Array[String]) {
    val myNick = randomNick
    val chan = "#VO1aW93A"
    //val client = IrcClient.connectSSL("irc.freenode.net", SSL_PORT, "UTF-8")
    val client = IrcClient.connect("irc.freenode.net", PLAINTEXT_PORT, "UTF-8")

    // connection level ops
    client.register(None, myNick, "bot", "ScalaBot1")

    //client.privmsg("NickServ", "identify")

    // channel level ops
    client.join(chan)

    // Block until the client terminates due to error or server action
    // The given handler will execute in the reader thread
    client.run { m =>
      (m.commandOrResponse,m.params) match {
        // Match this whether said in channel, or privately
        case ("PRIVMSG",_ :: "!part" :: _) =>
          client.part(chan, Some("Back in 3 secs"))
          Thread.sleep(3000)
          client.join(chan)
          true
        case ("PRIVMSG",_ :: "!flood" :: _) =>
          true
        // Match this only when said privately to me
        case ("PRIVMSG",`myNick` :: "!quit" :: _) =>
          client.quit(Some("I'm gone"))
          true  // hang on until server disconnects
        case _ => true
      }
    }

    client.disconnect
  }
}
