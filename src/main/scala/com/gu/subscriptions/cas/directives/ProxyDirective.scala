package com.gu.subscriptions.cas.directives

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.amazonaws.regions.{Regions, Region}
import com.gu.subscriptions.cas.config.Configuration
import com.gu.subscriptions.cas.config.Configuration.{proxyHost, proxyPort, proxyScheme}
import com.gu.subscriptions.cas.monitoring.{RequestMetrics, StatusMetrics}
import spray.can.Http
import spray.http.HttpHeaders.Host
import spray.http.{HttpRequest, HttpResponse}
import spray.routing.{Directives, RequestContext}

import scala.concurrent.Future
import scala.concurrent.duration._

trait ProxyDirective extends Directives{

  implicit val actorSystem: ActorSystem
  implicit val timeout: Timeout = 1.seconds

  import actorSystem.dispatcher

  val metrics = new CASMetrics(Configuration.stage)

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
    metrics.putRequest
    val excludeHeaders = Set("date", "content-type", "server", "content-length")
    (IO(Http) ? request).mapTo[HttpResponse].map { resp =>
      metrics.putResponseCode(resp.status.intValue, "POST")
      resp.withHeaders(resp.headers.filterNot(header => excludeHeaders.contains(header.lowercaseName)))
    }
  }
}

class CASMetrics(val stage: String) extends StatusMetrics with RequestMetrics {
  override val region: Region = Region.getRegion(Regions.EU_WEST_1)
  override val application: String = "CAS-legacy"
}