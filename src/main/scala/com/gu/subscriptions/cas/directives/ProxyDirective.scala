package com.gu.subscriptions.cas.directives

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.amazonaws.regions.{Region, Regions}
import com.gu.subscriptions.cas.config.Configuration
import com.gu.subscriptions.cas.config.Configuration.{proxyHost, proxyPort, proxyScheme}
import com.gu.subscriptions.cas.monitoring.{RequestMetrics, StatusMetrics}
import com.gu.subscriptions.cas.service.SubscriptionService.{extractZuoraSubscriptionId, verifySubscriptionExpiration}
import spray.can.Http
import spray.http.HttpHeaders.Host
import spray.http.MediaTypes._
import spray.http.{HttpRequest, HttpResponse}
import spray.routing.{Directives, RequestContext, Route}

import scala.concurrent.Future
import scala.concurrent.duration._

trait ProxyDirective extends Directives {

  implicit val actorSystem: ActorSystem

  val authRoute = path("auth") (post {
      respondWithMediaType(`application/json`) { requestContext =>
        proxyRequest(requestContext)
      }
    })

  val subsRoute = path("subs") (post {
      respondWithMediaType(`application/json`) { requestContext: RequestContext =>
        import scala.concurrent.ExecutionContext.Implicits.global
        import com.gu.subscriptions.cas.model.json.ModelJsonProtocol._
        import spray.json._
        extractZuoraSubscriptionId(requestContext.request.entity.asString)
          .fold(proxyRequest(requestContext))(subId =>
            requestContext.complete(verifySubscriptionExpiration(subId).map(_.toJson.toString())))
      }
    })

  val proxyRoute = authRoute ~ subsRoute

  val proxyRequest = {

    implicit val timeout: Timeout = 1.seconds
    import actorSystem.dispatcher
    val metrics = new CASMetrics(Configuration.stage)

    def sendReceive(request: HttpRequest, followRedirect: Boolean = true): Future[HttpResponse] = {
      metrics.putRequest
      val excludeHeaders = Set("date", "content-type", "server", "content-length")
      (IO(Http) ? request).mapTo[HttpResponse].map { resp =>
        metrics.putResponseCode(resp.status.intValue, "POST")
        resp.withHeaders(resp.headers.filterNot(header => excludeHeaders.contains(header.lowercaseName)))
      }
    }

    { ctx: RequestContext =>
        val newRequest = ctx.request.copy(
          uri = ctx.request.uri.withHost(proxyHost).withPort(proxyPort).withScheme(proxyScheme),
          headers = ctx.request.headers.map { header =>
            if (header.name.toLowerCase == "host") Host(proxyHost)
            else header
          })

        ctx.complete(sendReceive(newRequest))
    }
  }
}

class CASMetrics(val stage: String) extends StatusMetrics with RequestMetrics {
  override val region: Region = Region.getRegion(Regions.EU_WEST_1)
  override val application: String = "CAS-legacy"
}