package main.scala.bot1

// see http://tools.ietf.org/html/rfc2812

object IrcMessageParser {
  val NOSPCRLFCL =              """[^\000\015\012\ :]"""
  val MIDDLE     = NOSPCRLFCL + """[^\000\015\012\ ]*"""
  val TRAILING   =              """[^\000\015\012]*"""
  val Message = ("""(?ix)
                    (?: :((.+?)         # server|nick
                          ( (!(.+?))?   # optional user
                            @(.+?)      # host
                          )?            # optional user|host
                         )              # prefix
                         [\ ]
                    )?                  # prefix is optional
                    (([A-Z]+)|(\d+))    # command or response
                    ("""+TRAILING+""")  # params
                    \015\012
                 """).r
  val Params = (" ("+MIDDLE+")| :("+TRAILING+")").r

  def unapply(s:String):Option[IrcMessage] =
    s match {
      case Message(prefix, serverOrNick, bangUserAtHost, bangUser,
                   user, host, commandOrResponse, command, response, params) =>
        val paramList = Params.findAllIn(params).matchData.flatMap(
          _.subgroups match {
            case mid :: trail :: Nil => Option(mid).orElse(Option(trail))
            case _ => None
          } ).toList
        Some(IrcMessage(Option(prefix), Option(serverOrNick),
                        Option(bangUserAtHost), Option(bangUser),
                        Option(user), Option(host),
                        commandOrResponse, Option(command), Option(response),
                        paramList))
      case _ => None
    }

}