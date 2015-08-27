package com.gu.subscriptions.cas.directives

import com.gu.membership.zuora.soap.Zuora.Subscription
import com.gu.subscriptions.cas.model.json.ModelJsonProtocol._
import com.gu.subscriptions.cas.model.{SubscriptionExpiration, SubscriptionRequest}
import com.gu.subscriptions.cas.service.SubscriptionService
import org.joda.time.DateTime
import org.scalatest.FreeSpec
import spray.http.HttpHeaders._
import spray.http._
import spray.http.MediaTypes.`application/json`
import spray.json._
import spray.routing.{HttpService, Route}
import spray.testkit.ScalatestRouteTest
import spray.http.StatusCodes.BadRequest

import scala.concurrent.Future

class ProxyDirectiveSpec extends FreeSpec
with ScalatestRouteTest
with ProxyDirective
with HttpService {

  def actorRefFactory = system

  override implicit val actorSystem = system

  val handledByCAS = "Handled by CAS"
  override lazy val casRoute: Route = complete(handledByCAS)
  override lazy val proxyHost = "example-proxy"
  override lazy val proxyPort = 443
  override lazy val proxyScheme = "https"

  val now = DateTime.now()
  val termEndDate = now.plusYears(1)
  val expiration = SubscriptionExpiration(termEndDate)

  val validSubscription = Subscription(
    id = "123",
    name = "A-S123",
    accountId = "123",
    version = 1,
    termStartDate = now,
    termEndDate = termEndDate,
    contractAcceptanceDate = now,
    activationDate = None
  )

  override lazy val subscriptionService = new SubscriptionService {

    override def checkSubscriptionValidity(subscription: Subscription, postcode: String) =  Future { subscription.name.startsWith("A-S") }

    override def updateActivationDate(subscription: Subscription): Unit = ()

    override def getSubscription(name: String): Future[Option[Subscription]] = Future { Some(validSubscription) }
  }

  def inJson(r: Route): Route = respondWithMediaType(`application/json`) { r }

  "proxies the /auth requests to CAS" in {
    Post("/auth") ~> authRoute ~> check {
      assertResult(responseAs[String])(handledByCAS)
    }
  }

  "for the /subs endpoint" - {
    "when a valid request is made" - {
      "with a Zuora-formatted subscriber id" - {
        "returns the expiration" in {
          val payload = SubscriptionRequest(Some("A-S-id"), "password").toJson.toString()
          val req = HttpEntity(`application/json`, payload)

          Post("/subs", req) ~> inJson(subsRoute) ~> check {
            assertResult(expiration.toJson)(responseAs[String].parseJson)
          }
        }
      }

      "without a Zuora format" - {
        "proxies the request to CAS" in {
          val payload = SubscriptionRequest(Some("id"), "password").toJson.toString()
          val req = HttpEntity(`application/json`, payload)

          Post("/subs", req) ~> inJson(subsRoute) ~> check {
            assertResult(responseAs[String])(handledByCAS)
          }
        }
      }
    }

    "when an invalid subs request is made" - {
      "returns a bad request error" in {
        val req = HttpEntity(`application/json`, "invalid json")

        Post("/subs", req) ~> inJson(subsRoute) ~> check {
          assertResult(BadRequest)(status)
        }
      }
    }
  }

  "proxying to CAS" - {
    val fooHeader = RawHeader("foo", "bar")

    "proxyRequest" - {
      val req = HttpRequest(
        uri = Uri("http://example/endpoint"),
        headers = List(Host("www.example.com"), fooHeader)
      )

      "updates the URI with proxy info" in {
        assertResult(Uri("https://example-proxy:443/endpoint"))(proxyRequest(req).uri)
      }

      "updates the host header with the proxy host" in {
        assertResult(
          List(Host(proxyHost), fooHeader)
        )(
            proxyRequest(req).headers
          )
      }
    }

    "filterHeaders" - {
      "filters some headers out of the proxy response" in {
        val resp = HttpResponse(headers = List(
          Date(spray.http.DateTime.now),
          `Content-Type`(`application/json`),
          Server("Nginx"),
          `Content-Length`(1024L),
          fooHeader
        ))

        assertResult(List(fooHeader))(filterHeaders(resp).headers)
      }
    }
  }
}
