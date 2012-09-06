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

trait WordWrap {

  def wordWrap(text:String, wrapCol:Int, highlighter:String=>String):List[String] = {

    def fixEntities(s:String) =
      s.replace("&lt;",  "<")
       .replace("&gt;",  ">")
       .replace("&amp;", "&")

    val (_,lines,lastLine) =
      fixEntities(text).split(' ')
      .foldLeft((0,Nil:List[List[String]],Nil:List[String])) {
        (state,word) => {
          val (column,linesAcc,lineAcc) = state
          val newCol = column + 1 + word.size
          val colourWord = highlighter(word)
          if(column == 0)                   // always take first word
            (word.size, linesAcc, colourWord :: lineAcc)
          else if (newCol <= wrapCol)  // word fits on line
            (newCol, linesAcc, colourWord :: lineAcc)
          else                         // too long, wrap to next line
            (0, lineAcc :: linesAcc, List(colourWord))
        }
      }
    (lastLine :: lines).reverse.map { _.reverse.mkString(" ") }
  }

}