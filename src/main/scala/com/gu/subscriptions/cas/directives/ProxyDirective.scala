package com.gu.subscriptions.cas.directives

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.gu.subscriptions.cas.config.Configuration.{proxyHost, proxyPort, proxyScheme}
import com.gu.subscriptions.cas.monitoring.Metrics
import spray.can.Http
import spray.http.HttpHeaders.Host
import spray.http.StatusCodes.Redirection
import spray.http.{HttpRequest, HttpResponse}
import spray.httpx.RequestBuilding._
import spray.routing.{Directives, RequestContext}

import scala.concurrent.Future
import scala.concurrent.duration._

trait ProxyDirective extends Directives {

  implicit val actorSystem: ActorSystem
  implicit val timeout: Timeout = 1.seconds

  import actorSystem.dispatcher

  val proxyRoute =
    (path("subs") | path("auth")) {
      post { ctx: RequestContext =>
        val newRequest = ctx.request.copy(
          uri = ctx.request.uri.withHost(proxyHost).withPort(proxyPort).withScheme(proxyScheme),
          headers = ctx.request.headers.map { header =>
            if (header.name.toLowerCase == "host") Host(proxyHost)
            else header
          })

        ctx.complete(sendReceive(newRequest))
      }
    }

  def sendReceive(request: HttpRequest, followRedirect: Boolean = true): Future[HttpResponse] = {
    Metrics.put(s"Request URI ${request.uri.toString()}", 1)
    (IO(Http) ? request).mapTo[HttpResponse]
  }
}
