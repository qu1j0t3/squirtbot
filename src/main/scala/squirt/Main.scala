package main.scala.squirt

import util.Random
import math._

object SquirtBot {
  def randomNick = "s-%03d".format(abs(Random.nextInt) % 1000)
  def main(argv:Array[String]) {
    // All freenode servers listen on ports 6665, 6666, 6667,
    // 6697 (SSL only), 7000 (SSL only), 7070 (SSL only), 8000, 8001 and 8002.
    (new Bot("irc.freenode.net", 6667, "#ojXsKJOr", randomNick)).run
  }
}