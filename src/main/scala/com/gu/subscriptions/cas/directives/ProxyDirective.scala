package com.gu.subscriptions.cas.directives

import akka.actor.{ActorRef, ActorSystem}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.amazonaws.regions.{Region, Regions}
import com.gu.memsub.Subscription
import com.gu.memsub.Subscription.Name
import com.gu.subscriptions.cas.config.Configuration
import com.gu.subscriptions.cas.config.HostnameVerifyingClientSSLEngineProvider.provider
import com.gu.subscriptions.cas.directives.ResponseCodeTransformer._
import com.gu.subscriptions.cas.directives.ZuoraDirective._
import com.gu.subscriptions.cas.model.json.ModelJsonProtocol._
import com.gu.subscriptions.cas.model._
import com.gu.subscriptions.cas.monitoring.{Histogram, RequestMetrics, StatusMetrics}
import com.gu.subscriptions.cas.service.SubscriptionPersistenceService
import com.gu.subscriptions.cas.service.api.SubscriptionService
import com.typesafe.scalalogging.LazyLogging
import spray.can.Http
import spray.can.Http.HostConnectorSetup
import spray.http.HttpHeaders._
import spray.http.{HttpRequest, HttpResponse, Uri}
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
      // TODO first - a few days before authRoute is rewritten (to migrate expiry dates for as many active devices as possible):
        // if request contains app and device id
          // upsert the response record in DynamoDB (replacing the device's sub ID if necessary)
      ctx.complete(casResponse)
    }
  }

  val authRouteAppIdHistogram = new Histogram("authRouteAppIdHistogram", 1, DAYS) // how many app types?
  val authRouteExpiryDateHistogram = new Histogram("authRouteExpiryDate", 1, DAYS) // what variance of dates?

  val authRoute: Route = (path("auth") & post) {
    entity(as[AuthorisationRequest]) { subsReq =>
      subsReq.appId.foreach(authRouteAppIdHistogram.count)
      subsReq.expiryDate.foreach(authRouteExpiryDateHistogram.count)
      reject
    } ~ {
      casRoute
    }

    // TODO second - stop this cascading to casRoute. Algorithm is below.
      // if we have an app id and a device id, then lookup record in dynamo
        // if no record exists:
          // if there is an expiry date provided by the device which is < 1 year
            // upsert and return an AuthorisationResponse containing an Expiry(FREE, DEVICE_CONFIGURED, deviceDate) object
          // if there is no expiry date provided by the device
            // upsert and return an AuthorisationResponse containing Expiry(FREE, DEFAULT, 2-week-ahead-date).
        // if record exists
          // if expiry date is not set in request
            // return an AuthorisationResponse with an Expiry verbatim from the dynamo record)
          // if there is expiry date is in request --- SHOULD WE DO THIS STILL?
            // if dynamo record's Expiry has the DEVICE_CONFIGURED provider
              // if the expiry dates match
                // return an AuthorisationResponse with an Expiry verbatim from the dynamo record)
              // if the expiry dates don't match
                // return an AuthorisationResponse with Error: "Expiry date for free period has already been set by this device", code "auth.freeperiod.alreadyset"
            // if dynamo record's Expiry does *not* have DEVICE_CONFIGURED provider
              // create a ContentAuthorisation with Expiry(maintaining the expiryType, DEVICE_CONFIGURED, requestExpiry), upsert it and return an AuthorisationResponse
      // if no app id and device id
        // return an AuthorisationResponse with error: "Mandatory data missing from request" and code "mandatory.data.missing.error.code"
    // }
  }

  def zuoraRoute(subsReq: SubscriptionRequest): Route = zuoraDirective(subsReq) { (activation, subscriptionName) =>

    persistenceService.countInstallations(Credentials(subscriptionName, subsReq.password).get)

    val validSubscription = subscriptionService.getValidSubscription(Name(subscriptionName.get.trim.dropWhile(_ == '0')), subsReq.password.getOrElse(""))

    validSubscription.onFailure {
      case t: Throwable =>
        logger.error(s"Failed getting Zuora subscription ${t.getMessage} ${subsReq.subscriberId}")
        throw t
    }

    onSuccess(validSubscription) {
      case Some(subscription: Subscription) =>
        if (activation) { subscriptionService.updateActivationDate(subscription) }
        // TODO ASAP - if deviceId and appId are provided:
          // upsert a record in DynamoDB
        //Since the dates are in PST, we want to make sure that we don't cut any subscription one day short
        val zuoraExpiry = Expiry(subscription.termEndDate.plusDays(1).toDateTimeAtStartOfDay().toString, ExpiryType.SUB)
        val response = AuthorisationResponse(Some(zuoraExpiry))
        if (subsReq.hasValidAuth) {
          persistenceService.set(ContentAuthorisation(subsReq.appId.mkString, subsReq.deviceId.mkString, zuoraExpiry))
        }
        complete(response)
      case _ if subscriptionName.get.startsWith("A-S") =>
        //no point going to CAS if this is a Zuora sub
        notFound
      case _ =>
        //Go to CAS legacy /subs route
        reject
    }
  }

  val subsRoute = (path("subs") & post) {
    entity(as[SubscriptionRequest]) { subsReq =>
      // TODO third - handle limit of regisrations
        // get count of activations for these credentials from Dynamo
        // if count >= "max.subscriptions.per.user" return error: "Credentials used too often", credentials.overuse.error.code
        // else, continue, the zuoraRoute must update the count iff successful
      zuoraRoute(subsReq) ~ casRoute
    } ~ badRequest
  }
}

class CASMetrics(val stage: String) extends StatusMetrics with RequestMetrics {
  override val region: Region = Region.getRegion(Regions.EU_WEST_1)
  override val application: String = "CAS-legacy"
}