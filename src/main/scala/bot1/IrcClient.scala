package main.scala.bot1

import annotation.tailrec

import java.net.Socket
import javax.net.SocketFactory
import javax.net.ssl.SSLSocketFactory

import grizzled.slf4j.Logging

import java.util.concurrent._
import java.util.concurrent.atomic.AtomicInteger

class IrcClient(sockClient:IrcSocketClient) extends IrcClientInterface with Logging {

  val INTERMESSAGE_SLEEP_MS = 500 // avoid flooding Freenode
  val INTERGROUP_SLEEP_MS = 2000
  
  val ThrottleNotice = """\*\*\* Message to \S+ throttled due to flooding""".r

  val MircCode = """\002|(\003\d\d?(,\d\d?)?)|\017|\026|\037""".r
  def stripMircColours(s:String) = MircCode.replaceAllIn(s, "")

  // use a queue to decouple the message producers and consumer
  val throttledMessageQ = new LinkedBlockingQueue[IrcEvent](64)

  var messageCount = new AtomicInteger(0)
  var throttledCount = new AtomicInteger(0)

  override def getAndResetStats:Stats =
      Stats(messageCount.getAndSet(0), throttledCount.getAndSet(0))

  protected def unthrottledPrivmsg(target:String, msg:String) {
    messageCount.incrementAndGet
    command("PRIVMSG", List(target), Some(msg))
  }

  protected def unthrottledAction(target:String, action:String) {
    unthrottledPrivmsg(target, "\001ACTION %s\001".format(action))
  }

  def run(handle:IrcMessage => Boolean) {
    val dispatcher = new Runnable {
      def run {
        // synchronize on the channel name, so that multiple bots
        // configured in this program won't interrupt each other.
        // careful: will only work as expected if chan is an
        // intern'd String (e.g. a literal); see http://stackoverflow.com/a/9698305
        @tailrec
        def dispatchEvents(q:BlockingQueue[IrcEvent]) {
          q.take match {
            case IrcPrivMsg(chan, msg) => // skip sleep as these are meant to be isolated
              chan.synchronized { unthrottledPrivmsg(chan, msg) }
            case IrcAction(chan, actn) => // skip sleep as these are meant to be isolated
              chan.synchronized { unthrottledAction(chan, actn) }
            case IrcPrivMsgGroup(chan, group) =>
              chan.synchronized {
                group.foreach { msg =>
                  unthrottledPrivmsg(chan, msg)
                  Thread.sleep(INTERMESSAGE_SLEEP_MS) // this is meant to be the high-volume case
                }
                Thread.sleep(INTERGROUP_SLEEP_MS)
              }
          }
          dispatchEvents(q)
        }

        dispatchEvents(throttledMessageQ)
      }
    }

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
            debug("%8s %24s %24s | %s".format(msg.commandOrResponse,
                                              printFrom,
                                              if(dupe) "" else firstParam,
                                              stripMircColours(rest.mkString(" "))))
          case Nil => // no parameters
            debug("%8s %24s".format(msg.commandOrResponse, printFrom))
        }
      }

      // The client should mostly be blocked here,
      // waiting for data from the irc server.
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
                  case ("NOTICE",msgtarget :: ThrottleNotice() :: Nil) => // FIXME: match our nick
                    throttledCount.incrementAndGet
                    true
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
              warn("malformed message? "+reply)
              moreReplies(lastFrom, lastCommand, lastParam)
          }
        case None =>
          warn("input stream ended")
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

    // The dispatcher is set up as a FutureTask because we may want to
    // check if it has exited.
    new Thread(new FutureTask(dispatcher, true)).start
    
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

  def notice(target:String, msg:String) {
    command("NOTICE", List(target), Some(msg))
  }

  def privmsg(target:String, msg:String) {
    throttledMessageQ.put(IrcPrivMsg(target, msg))
  }

  def privmsgGroup(target:String, group:Seq[String]) {
    throttledMessageQ.put(IrcPrivMsgGroup(target, group))
  }

  def action(target:String, action:String) {
    throttledMessageQ.put(IrcAction(target, action))
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
  val SOCK_TIMEOUT_MS = 5*60*1000
  
  val sockFactory = SocketFactory.getDefault
  val sslSockFactory = SSLSocketFactory.getDefault

  //def withTimeout(sock:Socket) = { sock.setSoTimeout(SOCK_TIMEOUT_MS); sock }
  
  def connect(host:String, port:Int, charset:String) =
    new IrcClient(new IrcSocketClient(sockFactory.createSocket(host, port), charset))
  
  def connectSSL(host:String, port:Int, charset:String) =
    new IrcClient(new IrcSocketClient(sslSockFactory.createSocket(host, port), charset))
}