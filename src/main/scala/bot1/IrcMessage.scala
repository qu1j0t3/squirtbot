package main.scala.bot1

case class IrcMessage(
        prefix: Option[String],
        serverOrNick: Option[String],
        bangUserAtHost: Option[String],
        bangUser: Option[String],
        user: Option[String],
        host: Option[String],
        commandOrResponse: String,
        command: Option[String],
        response: Option[String],
        params: List[String]
        )
