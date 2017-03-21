package com.gu.subscriptions.cas.directives

import akka.actor.{ActorRef, ActorSystem}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.gu.memsub.Subscription.Name
import com.gu.memsub.subsv2.Subscription
import com.gu.memsub.subsv2.SubscriptionPlan.Paid
import com.gu.subscriptions.cas.config.Configuration
import com.gu.subscriptions.cas.config.HostnameVerifyingClientSSLEngineProvider.provider
import com.gu.subscriptions.cas.directives.ResponseCodeTransformer._
import com.gu.subscriptions.cas.directives.ZuoraDirective._
import com.gu.subscriptions.cas.model.json.ModelJsonProtocol._
import com.gu.subscriptions.cas.model._
import com.gu.subscriptions.cas.monitoring.{Histogram, RequestMetrics, StatusMetrics}
import com.gu.subscriptions.cas.service.api.{Error, SubscriptionService, Success => SuccessResponse}
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
import com.gu.cas.{PayloadResult, _}
import com.gu.scanamo.error.DynamoReadError
import com.gu.subscriptions.cas.service.DataStore
import org.joda.time.{DateTime, DateTimeZone}
import spray.httpx.marshalling.ToResponseMarshallable

import scala.util.{Failure, Success, Try}

trait ProxyDirective extends Directives with ErrorRoute with LazyLogging {

  implicit val actorSystem: ActorSystem
  implicit val timeout: Timeout = 3.seconds
  def subscriptionService: SubscriptionService
  def dataStore: DataStore

  def emergencyTokens: EmergencyTokens

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

  val authRouteAppIdHistogram = new Histogram("authRouteAppIdHistogram", 1, DAYS) // how many app types?
  val authRouteExpiryDateHistogram = new Histogram("authRouteExpiryDate", 1, DAYS) // what variance of dates?

  val authRoute: Route = (path("auth") & post) {
    entity(as[AuthorisationRequest]) { subsReq =>
      subsReq.appId.foreach(authRouteAppIdHistogram.count)
      subsReq.expiryDate.foreach(authRouteExpiryDateHistogram.count)

      (subsReq.deviceId, subsReq.appId) match {
        case (Some(deviceId), Some(appId)) =>

          val expiryResponse: Future[ToResponseMarshallable] = dataStore.getExpiration(appId = appId, deviceId = deviceId).map { getResponse =>
            getResponse match {
              case SuccessResponse(Some(expirationDate)) => AuthResponse(expirationDate)

              case SuccessResponse(None) =>
                val newExpiryDate = DateTime.now.plusWeeks(2)
                dataStore.setExpiration(appId, deviceId, newExpiryDate)
                AuthResponse(newExpiryDate)

              case Error(message) =>
                logger.error(s"dynamo db error for appId :$appId, deviceId: $deviceId, error message: $message")
                serverErrorResponse
            }
          }

          complete(expiryResponse)

        case _ => badRequest
      }
    }
  }


  def getEmergencyTokenExpiration(req: SubscriptionRequest): Option[SubscriptionExpiration] = {
    import com.gu.subscriptions.cas.model.TokenPayloadOps._

    req.subscriberId.flatMap { rawSubscriberId =>
      val subsId = rawSubscriberId.trim.toUpperCase

      if (!subsId.startsWith(emergencyTokens.prefix)) {
        None
      } else {
        logger.info(s"EMERGENCY PROVIDER triggered for subscriber id:'$subsId' password: '${req.password} deviceId:${req.deviceId}")

        Try(emergencyTokens.decoder.decode(subsId)) match {

          case Success(Valid(payload)) =>
            logger.info(s"subscriber id:'$subsId' resolves to $payload")
            logger.info(s"subscriber id:'$subsId' was created on ${payload.creationDate}")

            Some(SubscriptionExpiration(
              expiryDate = payload.expiryDate,
              expiryType = ExpiryType.SUB,
              subscriptionCode = Some(payload.subscriptionCode),
              provider = Some(Configuration.EmergencyTokens.prefix)
            ))

          case errorResponse =>
            logger.error(s"error decoding token $subsId :  $errorResponse")
            None
        }
      }
    }
  }

  def zuoraRoute(subsReq: SubscriptionRequest): Route = {
         zuoraDirective(subsReq) { (activation, subscriptionName) =>

        val validSubscription = subscriptionService.getValidSubscription(Name(subscriptionName.get.trim.dropWhile(_ == '0')), subsReq.password.getOrElse(""))

        validSubscription.onFailure {
          case t: Throwable =>
            logger.error(s"Failed getting Zuora subscription ${t.getMessage} ${subsReq.subscriberId}")
            throw t
        }

        onSuccess(validSubscription) {
          case Some(subscription: Subscription[Paid]) =>
            if (activation) {
              subscriptionService.updateActivationDate(subscription)
            }
            complete(SubscriptionExpiration(subscription.termEndDate.plusDays(1).toDateTimeAtStartOfDay(), ExpiryType.SUB))
          case _ => notFound
        }
      }
  }

  val subsRoute = (path("subs") & post) {
    entity(as[SubscriptionRequest]) { subsReq =>
      // TODO third - handle limit of registrations
        // get count of activations for these credentials from Dynamo
        // if count >= "max.subscriptions.per.user" return error: "Credentials used too often", credentials.overuse.error.code
        // else, continue, the zuoraRoute must update the count iff successful
      getEmergencyTokenExpiration(subsReq).map(complete(_)) getOrElse zuoraRoute(subsReq)
    } ~ badRequest
  }
}
