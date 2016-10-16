package squirt

import argonaut.Argonaut._
import argonaut._

import org.jibble.pircbot.Colors._

import WordWrap._

sealed trait Message

final case class Delete(id: String, userId: String) extends Message

final case class StatusWithheld(id: Long,
                                userId: Long,
                                withheldInCountries: List[String]) extends Message

final case class Disconnect(code: Int,
                            streamName: String,
                            reason: String) extends Message

final case class Friends(userIds: List[Long]) extends Message

final case class FavoriteEvent(source: TwitterUser, target: Tweet) extends Message

final case class FollowEvent(source: TwitterUser, target: TwitterUser) extends Message

case object IgnoredEvent extends Message


final case class Hashtag(text: String)

object Hashtag {
  implicit val decode: DecodeJson[Hashtag] =
    jdecode1L(Hashtag.apply)("text")
}


final case class Entities(hashtags: List[Hashtag], urls: List[TwitterUrl])

object Entities {
  implicit val decode: DecodeJson[Entities] =
    jdecode2L(Entities.apply)("hashtags", "urls")
}


final case class Tweet(text: String, id: String, user: TwitterUser, entities: Entities,
                       retweetOf: Option[Tweet], withheldInCountries: Option[List[String]])
  extends Message {

  def url:String = "https://twitter.com/" + user.screenName + "/status/" + id

  val indentCols = 17
  val wrapCols   = 57

  def highlight(word:Word) = word match {
    case UserMentionWord(s) => MAGENTA + s + NORMAL
    case HashTagWord(s)     => CYAN    + s + NORMAL
    case UrlWord(s)         => PURPLE  + s + NORMAL
    case PlainWord(s)       => s
  }
  def highlightUrl(s:String)     = DARK_GREEN + s + NORMAL
  def highlightNick(s:String)    = BOLD       + s + NORMAL
  def highlightLeftCol(s:String) = DARK_BLUE  + s + NORMAL

  def fixEntities(s:String) =
    s.replace("&lt;",  "<")
      .replace("&gt;",  ">")
      .replace("&amp;", "&")

  def splitWithIndex(s:String, c:Char):List[(Int,String)] =
    s.split(c).foldLeft( (0,Nil:List[(Int,String)]) )(
      (acc, s) =>
        acc match { case (col,rest) => (col + s.size + 1, (col,s) :: rest) }
    )._2.reverse

  def format(leftColumn:List[String], text:String):List[String] = {
    // 1) convert string into lines. keep starting column for each line
    // 2) break lines into words. classify each word. match each word
    //    to URL entity indexes; if matched, add parenthesised hostname
    // 3) stringify each word with colour codes, then flatMap

    // respect newlines in original tweet
    val wrapped:List[String] =
      splitWithIndex(text, '\n').flatMap {
        case (lineIdx,line) => {
          val words:List[Word] = splitWithIndex(line, ' ').flatMap {
            case (wordIdx,word) => // could possibly substitute the t.co url with expanded if it was shorter
              classify(fixEntities(word)) ::
                entities.urls.filter( u => ((c:Int) => c > 0 && c <= word.size)(u.endIndex - (lineIdx + wordIdx)) )
                  .map( u => UrlWord("(" + UrlResolver.resolve(u.expandedUrl).getOrElse("error") + ")") )
          }
          wrap(words, wrapCols).map( _.map(highlight(_)) )
        }
      }.map(_.mkString(" "))

    leftColumn.zipAll(highlightUrl(url) :: wrapped, "", "").zipWithIndex.map {
      case ((a,b),i) =>
        (if(i == 0) highlightNick(a) else highlightLeftCol(a)) +
          " "*(2 max (indentCols - a.size)) + b
    } ++ List(" ")
  }

  def description:String =
    retweetOf.fold("tweet by @"+user.screenName)(rt =>
      "retweet of @%s by @%s".format(rt.user.screenName, user.screenName))

  def descriptionList:List[String] =
    retweetOf.fold(List("@"+user.screenName))(rt =>
      List("@"+rt.user.screenName,
           " retweeted by",
           " @"+user.screenName))

  def abbreviated = {
    val textFixed = fixEntities(text)
    val abbrev = textFixed.split("\\s").take(8).mkString(" ")
    if(abbrev != textFixed) abbrev+"..." else abbrev
  }

  // If it's a retweet, send full text of original tweet,
  // otherwise full text of this tweet
  def sendTweet: List[String] =
    format(descriptionList, retweetOf.fold(text)(_.text)) ++
      withheldInCountries.fold[List[String]](Nil){
        case Nil => Nil
        case cs => List("Withheld from: " + cs.mkString(", "))
      }
}

object Tweet {
  // We can't describe this in the obvious way, because of infinite
  // recursion on the decoder parameter for Tweet -- i.e. not like this:
  //   jdecode6L(Tweet.apply)("text", "id_str", "user", "entities",
  //                          "retweeted_status", "withheld_in_countries")
  implicit val decode: DecodeJson[Tweet] =
    DecodeJson(c =>
      for {
        t  <- (c --\ "text").as[String]
        id <- (c --\ "id_str").as[String]
        u  <- (c --\ "user").as[TwitterUser]
        e  <- (c --\ "entities").as[Entities]
        rt <- (c --\ "retweeted_status").as[Option[Tweet]]
        w  <- (c --\ "withheld_in_countries").as[Option[List[String]]]
      } yield Tweet(t, id, u, e, rt, w)
    )
}


sealed trait EventTarget

case object NullTarget extends EventTarget
case class TweetTarget(t: Tweet) extends EventTarget
case class UserTarget(u: TwitterUser) extends EventTarget

object EventTarget {
  implicit val decode: DecodeJson[EventTarget] =
    DecodeJson(c =>
      c.as[Unit].map[EventTarget](_ => NullTarget) |||
      c.as[TwitterUser].map[EventTarget](UserTarget) |||
      c.as[Tweet].map[EventTarget](TweetTarget)
    )
}


object Message {
  def delete(id: String, userId: String): Message = Delete(id, userId)

  def statusWithheld(id: Long, userId: Long, withheldInCountries: List[String]): Message =
    StatusWithheld(id, userId, withheldInCountries)

  def disconnect(code: Int, stream_name: String, reason: String): Message =
    Disconnect(code, stream_name, reason)

  def friends(ids: List[Long]): Message = Friends(ids)

  def event(event: String, source: TwitterUser, target: EventTarget): Message =
    (event,target) match {
      case ("favorite",TweetTarget(t)) => FavoriteEvent(source, t)
      case ("follow",UserTarget(u)) => FollowEvent(source, u)
      case _ => IgnoredEvent
    }

  implicit val decode: DecodeJson[Message] =
    DecodeJson(c =>
      (c --\ "delete" --\ "status").as(jdecode2L(delete)("id_str", "user_id_str")) |||
      (c --\ "status_withheld").as(jdecode3L(statusWithheld)("id", "user_id", "withheld_in_countries")) |||
      (c --\ "disconnect").as(jdecode3L(disconnect)("code", "stream_name", "reason")) |||
      (c --\ "friends").as[List[Long]].map(friends) |||
      c.as[Tweet].map(t => t:Message) |||
      c.as(jdecode3L(event)("event", "source", "target"))
    )
}
