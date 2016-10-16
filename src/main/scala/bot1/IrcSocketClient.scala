package main.scala.bot1

import annotation.tailrec

import java.net.Socket
import java.net.SocketTimeoutException

import grizzled.slf4j.Logging

class IrcSocketClient(sock:Socket, charset:String) extends Logging {
  val CR = 13
  val LF = 10
  val MESSAGE_LIMIT = 512

  val oStream = sock.getOutputStream
  val iStream = sock.getInputStream

  def getReply:Option[String] = {
    val message = new Array[Byte](MESSAGE_LIMIT)

    // This transition function recognises a CR/LF sequence.
    def nextState(state:Int, b:Int) = (state,b) match {
      case (0,CR) => 1  // saw CR
      case (1,LF) => 2  // saw LF after CR
      case _      => 0  // anything else, reset
    }

    // Eat all input until CR/LF seen.
    @tailrec
    def skipToCrLf(state:Int):Boolean =
      iStream.read match {
        case -1 => false
        case b =>
          val state1 = nextState(state, b)
          state1 == 2 || skipToCrLf(state1)
      }

    // Collect next byte into message. Return None if the stream has ended.
    // WARNING: This may throw IOException
    @tailrec
    def getByte(i:Int, state:Int):Option[String] =
      iStream.read match {
        case -1 => None // fatal error
        case 0 => // malformed message. skip until CR/LF and then retry.
          if(skipToCrLf(0)) getByte(0, 0) else None
        case b =>
          message(i) = b.toByte
          val state1 = nextState(state, b)
          if(state1 == 2) {
            Some(new String(message, 0, i+1, charset))
          } else if((i-state1) > MESSAGE_LIMIT-2) { // message is over limit
            if(skipToCrLf(state1)) getByte(0, 0) else None
          } else {
            getByte(i+1, state1)
          }
      }

    getByte(0, 0)

    /* If using a socket timeout:
    try {
      getByte(0, 0)
    }
    catch {
      case e:SocketTimeoutException =>
        debug(e)
        getReply  // retry [This doesn't compile in 2.9.x, can't tailrec]
      // Let's assume we just missed a PING because the connection
      // was busy in the other direction
    }*/
  }

  // Parameters must follow the lexical rules given in RFC.
  // In particular, "middle" parameters may not begin with space or :
  // Optional single "trailing" parameter is prefixed by : and can
  // contain spaces and :'s. No parameter can include NUL, CR or LF.

  def command(cmd:String, middle:Seq[String], trailing:Option[String]) {
    val message = cmd+" "+middle.mkString(" ")+trailing.map(" :"+_).getOrElse("")
    oStream.synchronized {
      debug("Sending: "+message)
      oStream.write(message.getBytes(charset))
      oStream.write(CR)
      oStream.write(LF)
      oStream.flush
    }
  }
        
  // Just disconnect. We might do this if the server has disconnected us.
  def disconnect { sock.close }
}