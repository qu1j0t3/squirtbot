/*
    This file is part of "squirtbot", a simple Scala irc bot
    Copyright (C) 2012 Toby Thain, toby@telegraphics.com.au

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

object WordWrap {

  sealed trait Word { def text:String }
  case class PlainWord(text:String) extends Word
  case class UrlWord(text:String) extends Word
  case class HashTagWord(text:String) extends Word
  case class UserMentionWord(text:String) extends Word

  val ScreenName = """@.+""".r
  val HashTag    = """#.+""".r
  val Url        = """https?:\/\/.*""".r

  def classify(s:String):Word = s match {
    case ScreenName() => UserMentionWord(s)
    case HashTag()    => HashTagWord(s)
    case Url()        => UrlWord(s)
    case _            => PlainWord(s)
  }

  def wrap(words:List[Word], wrapCol:Int):List[List[Word]] = {
    val (_,lines,lastLine) =
      words.foldLeft((0, Nil:List[List[Word]], Nil:List[Word])) {
        (state,word) => {
          val (column,linesAcc,lineAcc) = state
          val newCol = column + 1 + word.text.size
          if(column == 0)              // always take first word
            (word.text.size, linesAcc, word :: lineAcc)
          else if (newCol <= wrapCol)  // word fits on line
            (newCol, linesAcc, word :: lineAcc)
          else                         // too long, wrap to next line
            (word.text.size, lineAcc :: linesAcc, List(word))
        }
      }
    (lastLine :: lines).reverse.map(_.reverse)
  }

}