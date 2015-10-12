package com.gu.subscriptions.cas.directives

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.amazonaws.regions.{Region, Regions}
import com.gu.subscriptions.cas.config.Configuration
import com.gu.subscriptions.cas.directives.ResponseCodeTransformer._
import com.gu.subscriptions.cas.directives.ZuoraDirective._
import com.gu.subscriptions.cas.model.json.ModelJsonProtocol._
import com.gu.subscriptions.cas.model.{SubscriptionExpiration, SubscriptionRequest}
import com.gu.subscriptions.cas.monitoring.{RequestMetrics, StatusMetrics}
import com.gu.subscriptions.cas.service.SubscriptionService
import com.gu.subscriptions.cas.service.zuora.ZuoraSubscriptionService
import spray.can.Http
import spray.can.Http.HostConnectorSetup
import spray.http.HttpHeaders._
import spray.http.{HttpRequest, HttpResponse}
import spray.httpx.ResponseTransformation._
import spray.httpx.SprayJsonSupport._
import spray.routing.{Directives, Route}
import com.gu.subscriptions.cas.config.HostnameVerifyingClientSSLEngineProvider.provider
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

trait ProxyDirective extends Directives with ErrorRoute {

  implicit val actorSystem: ActorSystem
  lazy val subscriptionService: SubscriptionService = ZuoraSubscriptionService
  lazy val proxyHost = Configuration.proxyHost
  lazy val proxyPort = Configuration.proxyPort
  lazy val proxyScheme = Configuration.proxyScheme

  def proxyRequest(in: HttpRequest): HttpRequest =
    in.copy(
      uri = in.uri.withHost(proxyHost).withPort(proxyPort).withScheme(proxyScheme),
      headers = in.headers.map {
        case Host(_, _) => Host(proxyHost)
        case header => header
      }
    )

  def logProxyResp(metrics: CASMetrics): HttpResponse => HttpResponse = { resp =>
    metrics.putResponseCode(resp.status.intValue, "POST")
    resp
  }

  val filterHeaders: HttpResponse => HttpResponse = resp =>
    resp.withHeaders(resp.headers.filter {
      case Date(_) | `Content-Type`(_) | Server(_) | `Content-Length`(_) => false
      case _ => true
    })

  lazy val casRoute: Route = {
    implicit val timeout: Timeout = 1.seconds
    val metrics = new CASMetrics(Configuration.stage)

    def sendReceive(request: HttpRequest, followRedirect: Boolean = true): Future[HttpResponse] = {
      (IO(Http) ? (request, HostConnectorSetup(proxyHost, proxyPort, sslEncryption = true) )).mapTo[HttpResponse].map(
        logProxyResp(metrics) ~>
          filterHeaders ~>
          changeResponseCode
      )
    }

    ctx => ctx.complete(sendReceive(proxyRequest(ctx.request)))
  }

  val authRoute: Route = (path("auth") & post)(casRoute)

  def zuoraRoute(subsReq: SubscriptionRequest): Route = zuoraDirective(subsReq) { subscriptionName =>

    val validSubscription = subscriptionService.getValidSubscription(subscriptionName, subsReq.password)

    onSuccess(validSubscription) {
      case Some(subscription) =>
        subscriptionService.updateActivationDate(subscription)
        complete(SubscriptionExpiration(subscription.termEndDate))
      case _ =>
        notFound
    }
  }

  val subsRoute = (path("subs") & post) {
    entity(as[SubscriptionRequest]) { subsReq =>
      zuoraRoute(subsReq) ~ casRoute
    } ~ badRequest
  }
}

class CASMetrics(val stage: String) extends StatusMetrics with RequestMetrics {
  override val region: Region = Region.getRegion(Regions.EU_WEST_1)
  override val application: String = "CAS-legacy"
}