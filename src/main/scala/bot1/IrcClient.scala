package main.scala.bot1

import annotation.tailrec
import concurrent.ops.spawn

import javax.net.SocketFactory
import javax.net.ssl.SSLSocketFactory

import main.scala.squirt.Signal

class IrcClient(sockClient:IrcSocketClient) extends IrcClientInterface {

  val MircCode = """\002|(\003\d\d?(,\d\d?)?)|\017|\026|\037""".r
  def stripMircColours(s:String) = MircCode.replaceAllIn(s, "")

  def run(handle:IrcMessage => Boolean) {
    val exit = new Signal

    @tailrec
    def moreReplies(lastFrom:Option[String],
                    lastCommand:Option[String],
                    lastParam:Option[String]) {
      def logMessage(msg:IrcMessage) {
          val dupe = lastFrom == msg.serverOrNick &&
                     lastCommand.exists(msg.commandOrResponse ==) &&
                     lastParam == msg.params.headOption
          // For clarity, don't print the 'from' field or first parameter
          // if they are unchanged AND the command is unchanged.
          val printFrom = if(dupe) "" else msg.serverOrNick.getOrElse("")
          msg.params match {
            case firstParam :: rest => // at least one parameter
              println("%8s %24s %24s | %s".format(msg.commandOrResponse,
                                                  printFrom,
                                                  if(dupe) "" else firstParam,
                                                  stripMircColours(rest.mkString(" "))))
            case Nil => // no parameters
              println("%8s %24s".format(msg.commandOrResponse, printFrom))
          }
      }

      sockClient.getReply match {
        case Some(reply) =>
          reply match {
            case IrcMessageParser(msg) =>
              // log the incoming message in a semi-friendly format
              logMessage(msg)

              // handle message ------------------------------------------
              val continue =
                (msg.commandOrResponse,msg.params) match {
                  case ("PING",server1 :: _) =>
                    command("PONG", List(server1), None); true
                  case _ =>
                    handle(msg)
                }

              // the recursive call cannot be done from a closure (e.g. argument
              // to foreach), due to limitations of Scala tail call opt
              if(continue)
                moreReplies(msg.serverOrNick,
                            Option(msg.commandOrResponse),
                            msg.params.headOption)
            case _ =>
              println("malformed message? "+reply)
              moreReplies(lastFrom, lastCommand, lastParam)
          }
        case None => println("input stream ended")
      }
    }

    spawn {
      try {
        moreReplies(None, None, None)
      }
      catch {
        case e:Exception => println(e.getMessage)
      }
      exit.signal
    }
    exit.await
  }

  def command(cmd:String, middle:Seq[String], trailing:Option[String]) {
    sockClient.command(cmd, middle, trailing)
  }

  def register(pass:Option[String], nick:String, user:String, realName:String) {
    pass.foreach(pass => command("PASS", List(pass), None))
    command("NICK", List(nick), None)
    command("USER", List(user, "0", "*"), Some(realName))
  }

  def join(chan:String) {
    command("JOIN", List(chan), None)
  }

  def part(chan:String, msg:Option[String]) {
    command("PART", List(chan), msg)
  }

  def quit(msg:Option[String]) {
    command("QUIT", Nil, msg)
    // It would now be polite to wait for the server to disconnect.
  }

  def privmsg(target:String, msg:String) {
    command("PRIVMSG", List(target), Some(msg))
  }
  
  // Just disconnect. We might do this if the server has disconnected us.
  def disconnect { sockClient.disconnect }
}

object IrcClient {
  val sockFactory = SocketFactory.getDefault
  val sslSockFactory = SSLSocketFactory.getDefault
  
  def connect(host:String, port:Int) =
    new IrcClient(new IrcSocketClient(sockFactory.createSocket(host, port)))
  def connectSSL(host:String, port:Int) =
    new IrcClient(new IrcSocketClient(sslSockFactory.createSocket(host, port)))
}