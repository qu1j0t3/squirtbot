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

import io.Source
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.fluent._

object Ur1Ca extends UrlShortener {
  protected lazy val client = new DefaultHttpClient

  def shortenUrl(longUrl:String):Option[String] = {
    // Parsing HTML with a Regex is never a good idea! I defend it here
    // because we are dealing with a single specific input document.
    val ShortUrl = """^.*Your ur1 is: <a href="(.*?)">.*$""".r

    try { // on flaky networks, this may abort with org.apache.http.NoHttpResponseException, etc
      val resp = Request.Post("http://ur1.ca/")
                        .bodyForm(Form.form.add("longurl", longUrl).build)
                        .execute
                        .returnResponse
      val status = resp.getStatusLine.getStatusCode
      if(status >= 200 && status < 300)
        Source.fromInputStream(resp.getEntity.getContent, "UTF-8")
        .getLines.collectFirst { case ShortUrl(url) => url }
      else
        None
    } catch {
      case _ => None
    }
  }
}
