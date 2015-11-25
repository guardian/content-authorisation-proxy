package com.gu.subscriptions.cas.directives

import akka.actor.{ActorRef, ActorSystem}
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
import com.typesafe.scalalogging.LazyLogging
import spray.can.Http
import spray.can.Http.HostConnectorSetup
import spray.http.HttpHeaders._
import spray.http.{HttpRequest, HttpResponse, Uri}
import spray.httpx.ResponseTransformation._
import spray.httpx.SprayJsonSupport._
import spray.routing.{Directives, Route}
import com.gu.subscriptions.cas.config.HostnameVerifyingClientSSLEngineProvider.provider
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

trait ProxyDirective extends Directives with ErrorRoute with LazyLogging {

  implicit val actorSystem: ActorSystem
  implicit val timeout: Timeout = 2.seconds
  lazy val io: ActorRef = IO(Http)
  lazy val subscriptionService: SubscriptionService = ZuoraSubscriptionService


  def proxyRequest(in: HttpRequest, proxy: String): Future[HttpResponse] = {
    val metrics = new CASMetrics(Configuration.stage)
    val (proxyScheme, proxyHost, proxyPort, proxyPath) = {
      val proxyUri = Uri(proxy)
      (proxyUri.scheme, proxyUri.authority.host.address, proxyUri.effectivePort, proxyUri.path)
    }

    val request = in.copy(
      uri = in.uri.withHost(proxyHost).withPort(proxyPort).withScheme(proxyScheme).withPath(Uri.Path(proxyPath.toString() + in.uri.path.toString())),
      headers = in.headers.map {
        case Host(_, _) => Host(proxyHost)
        case header => header
      }
    )
    val hostConnectorSetup = HostConnectorSetup(proxyHost, proxyPort, sslEncryption = true)
    (io ?(request, hostConnectorSetup)).mapTo[HttpResponse].map(
      logProxyResp(metrics) ~>
        filterHeaders ~>
        changeResponseCode
    )
  }

  def logProxyResp(metrics: CASMetrics): HttpResponse => HttpResponse = { resp =>
    metrics.putRequest
    metrics.putResponseCode(resp.status.intValue, "POST")
    resp
  }

  val filterHeaders: HttpResponse => HttpResponse = resp =>
    resp.withHeaders(resp.headers.filter {
      case Date(_) | `Content-Type`(_) | Server(_) | `Content-Length`(_) => false
      case _ => true
    })

  private def compareSubscriptionRequests(request: HttpRequest, casResponse: Future[HttpResponse]): Unit = {
    val uri: Uri = request.uri
    val isSubsRequest = uri.path != null &&
      uri.path.toString().contains("/subs")

    def compareResponses(oldResponse: HttpResponse, newResponse: HttpResponse) {
      val hasSameStatus = oldResponse.status == newResponse.status
      val hasSameBody = oldResponse.entity.asString == newResponse.entity.asString
      if (!hasSameStatus && !hasSameBody)
        logger.error("Legacy apps returned different Responses")
      else
        logger.info("Legacy apps returned same Responses ")
    }

    if (isSubsRequest) {
      logger.info("Subs request made, comparing old with new CAS instances")
      for {
        oldResponse <- casResponse
        newResponse <- proxyRequest(request, Configuration.proxyNew)
      } yield compareResponses(oldResponse, newResponse)
    }
  }

  lazy val casRoute: Route = {
    ctx => {
      val request: HttpRequest = ctx.request
      val casResponse = proxyRequest(request, Configuration.proxy)
      ctx.complete(casResponse)
      try {
        compareSubscriptionRequests(request, casResponse)
      }
      catch {
        case e: Exception => logger.error("Error comparing SubscriptionRequests", e)
      }
    }
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