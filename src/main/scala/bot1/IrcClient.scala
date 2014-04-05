package main.scala.bot1

import annotation.tailrec
import concurrent.ops.spawn

import javax.net.SocketFactory
import javax.net.ssl.SSLSocketFactory

class IrcClient(sockClient:IrcSocketClient) extends IrcClientInterface {

  val MircCode = """\002|(\003\d\d?(,\d\d?)?)|\017|\026|\037""".r
  def stripMircColours(s:String) = MircCode.replaceAllIn(s, "")

  def run(handle:IrcMessage => Boolean) {
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
        /* e.g. server might disconnect us for flooding:
Sending: PRIVMSG #VO1aW93A :02 @kim_tastiic    information
    QUIT                    s-229             Excess Flood |
   ERROR                          Closing Link: dsl-xx-xx-xx-xx.acanac.net (Excess Flood) |
input stream ended
Disconnect
Sending: PRIVMSG #VO1aW93A :03........................................  http://ur1.ca/g8oqx
Sending: PRIVMSG #VO1aW93A :Socket closed
         */
      }
    }

    moreReplies(None, None, None)
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

  def action(target:String, action:String) {
    privmsg(target, "\001ACTION %s\001".format(action))
  }
  
  // Just disconnect. We might do this if the server has disconnected us.
  def disconnect { sockClient.disconnect }
}

object IrcClient {
  val FREENODE = "irc.freenode.net"
  
  // All freenode servers listen on ports 6665, 6666, 6667,
  // 6697 (SSL only), 7000 (SSL only), 7070 (SSL only), 8000, 8001 and 8002.
  val PLAINTEXT_PORT = 6667
  val SSL_PORT = 6697
  
  val sockFactory = SocketFactory.getDefault
  val sslSockFactory = SSLSocketFactory.getDefault
  
  def connect(host:String, port:Int, charset:String) =
    new IrcClient(new IrcSocketClient(sockFactory.createSocket(host, port), charset))
  
  def connectSSL(host:String, port:Int, charset:String) =
    new IrcClient(new IrcSocketClient(sslSockFactory.createSocket(host, port), charset))
}