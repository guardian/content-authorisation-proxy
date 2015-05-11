package com.gu.subscriptions.cas.directives

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.gu.subscriptions.cas.Configuration.{proxyHost, proxyPort, proxyScheme}
import spray.can.Http
import spray.http.HttpHeaders.Host
import spray.http.{HttpRequest, HttpResponse}
import spray.httpx.RequestBuilding._
import spray.routing.{Directives, RequestContext}

import scala.concurrent.Future
import scala.concurrent.duration._

trait ProxyDirective extends Directives {

  implicit val actorSystem: ActorSystem
  implicit val timeout: Timeout = 1.seconds

  import actorSystem.dispatcher

  val securePort = 443
  val proxyRoute = (get|post) { ctx: RequestContext =>

    val newRequest = ctx.request.copy(
      uri = ctx.request.uri.withHost(proxyHost).withPort(proxyPort).withScheme(proxyScheme),
      headers = ctx.request.headers.map { header =>
        if (header.name.toLowerCase == "host") Host(proxyHost)
        else header
      })

    ctx.complete(sendReceive(newRequest))
  }

  def sendReceive(request: HttpRequest, followRedirect: Boolean = true): Future[HttpResponse] =
    (IO(Http) ? request)
      .mapTo[HttpResponse]
      .flatMap { response =>
        val isRedirect = response.status.intValue / 100 == 3
        response.headers.find(followRedirect && isRedirect && _.name.toLowerCase == "location")
          .map(loc => sendReceive(Get(loc.value)))
          .getOrElse(Future.successful(response))
      }
}
