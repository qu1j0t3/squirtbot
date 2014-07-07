package main.scala.squirt

import org.apache.http.impl.client.HttpClients
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIUtils

import grizzled.slf4j.Logging

object UrlResolver extends Logging {

  def resolve(url:String):Option[String] = {
    try {
      val context = HttpClientContext.create
      val response = HttpClients.createDefault.execute(new HttpGet(url), context)
      val target = context.getTargetHost.getHostName
      response.close
      Some(target)
    } catch {
      case _:Exception => None
    }
  }

}