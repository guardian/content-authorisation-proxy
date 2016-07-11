package com.gu.subscriptions.cas.directives

import akka.actor.{ActorRef, ActorSystem}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.amazonaws.regions.{Region, Regions}
import com.gu.memsub.Subscription.Name
import com.gu.subscriptions.cas.config.Configuration
import com.gu.subscriptions.cas.config.HostnameVerifyingClientSSLEngineProvider.provider
import com.gu.subscriptions.cas.directives.ResponseCodeTransformer._
import com.gu.subscriptions.cas.directives.ZuoraDirective._
import com.gu.subscriptions.cas.model.json.ModelJsonProtocol._
import com.gu.subscriptions.cas.model.{SubscriptionExpiration, SubscriptionRequest}
import com.gu.subscriptions.cas.monitoring.{Histogram, RequestMetrics, StatusMetrics}
import com.gu.subscriptions.cas.service.api.SubscriptionService
import com.typesafe.scalalogging.LazyLogging
import spray.can.Http
import spray.can.Http.HostConnectorSetup
import spray.http.HttpHeaders._
import spray.http.{HttpHeader, HttpRequest, HttpResponse, Uri}
import spray.httpx.ResponseTransformation._
import spray.httpx.SprayJsonSupport._
import spray.routing.{Directives, Route}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._


trait ProxyDirective extends Directives with ErrorRoute with LazyLogging {

  implicit val actorSystem: ActorSystem
  implicit val timeout: Timeout = 3.seconds
  def subscriptionService: SubscriptionService

  lazy val io: ActorRef = IO(Http)
  val filterHeaders: HttpResponse => HttpResponse = resp =>
    resp.withHeaders(resp.headers.filter {
      case Date(_) | `Content-Type`(_) | Server(_) | `Content-Length`(_) => false
      case _ => true
    })

  def connectorFromUrl(proxyUri: Uri) = {
    HostConnectorSetup(proxyUri.authority.host.address, proxyUri.effectivePort, sslEncryption = proxyUri.scheme.toLowerCase().equals("https"))
  }

  def createProxyRequest(in: HttpRequest, proxyUri: Uri) = {
    val (proxyScheme, proxyHost, proxyPort, _) = {
      (proxyUri.scheme, proxyUri.authority.host.address, proxyUri.effectivePort, proxyUri.path)
    }
    in.copy(
      uri = in.uri.withHost(proxyHost).withPort(proxyPort).withScheme(proxyScheme),
      headers = in.headers.map {
        case Host(_, _) => Host(proxyHost)
        case header => header
      }.filter {
        case `Content-Type`(_) => false
        case `Content-Length`(_) => false
        case _ => true
      }
    )
  }

  def proxyRequest(in: HttpRequest, proxy: String, metrics: CASMetrics): Future[HttpResponse] = {
    val proxyUri = Uri(proxy)
    val request = createProxyRequest(in, proxyUri)
    val hostConnectorSetup = connectorFromUrl(proxyUri)
    val out = (io ?(request, hostConnectorSetup)).mapTo[HttpResponse].map(
      logProxyResp(metrics) ~>
        filterHeaders ~>
        changeResponseCode
    )
    out.onFailure {
      case t: Throwable =>
        logger.error(s"${t.getMessage} asking CAS about a subscription")
        throw t
    }

    out
  }

  def logProxyResp(metrics: CASMetrics): HttpResponse => HttpResponse = { resp =>
    metrics.putRequest
    metrics.putResponseCode(resp.status.intValue, "POST")
    resp
  }

  lazy val casRoute: Route = {
    val metrics = new CASMetrics(Configuration.stage)
    ctx => {
      val request: HttpRequest = ctx.request
      val casResponse = proxyRequest(request, Configuration.proxy, metrics)
      ctx.complete(casResponse)
    }
  }

  val authRoute: Route = (path("auth") & post)(casRoute)

  val subsRouteHistogram = new Histogram("subsRoute", 1, DAYS)
  val zuoraRouteHistogram = new Histogram("zuoraRouteFound", 1, DAYS)

  def zuoraRoute(subsReq: SubscriptionRequest): Route = zuoraDirective(subsReq) { (activation, subscriptionName) =>
    val validSubscription = subscriptionService.getValidSubscription(Name(subscriptionName.get.dropWhile(_ == '0')), subsReq.password)

    validSubscription.onFailure {
      case t: Throwable =>
        logger.error(s"Failed getting Zuora subscription ${t.getMessage} ${subsReq.subscriberId}")
        throw t
    }

    onSuccess(validSubscription) {
      case Some(subscription) =>
        if (activation) { subscriptionService.updateActivationDate(subscription) }
        subsReq.subscriberId.foreach(zuoraRouteHistogram.count) // requested ID, not Subscription.Name
        //Since the dates are in PST, we want to make sure that we don't cut any subscription one day short
        complete(SubscriptionExpiration(subscription.termEndDate.plusDays(1).toDateTimeAtStartOfDay()))
      case _ if subscriptionName.get.startsWith("A-S") => notFound //no point going to CAS if this is a Zuora sub
      case _ => reject
    }
  }

  val subsRoute = (path("subs") & post) {
    entity(as[SubscriptionRequest]) { subsReq =>
      subsReq.subscriberId.foreach(subsRouteHistogram.count)
      zuoraRoute(subsReq) ~ casRoute
    } ~ badRequest
  }
}

class CASMetrics(val stage: String) extends StatusMetrics with RequestMetrics {
  override val region: Region = Region.getRegion(Regions.EU_WEST_1)
  override val application: String = "CAS-legacy"
}