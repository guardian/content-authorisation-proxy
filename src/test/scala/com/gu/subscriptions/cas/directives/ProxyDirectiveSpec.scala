package com.gu.subscriptions.cas.directives

import akka.testkit.TestProbe
import com.gu.i18n.GBP
import com.gu.memsub.Subscription
import com.gu.memsub.Subscription.{Name, ProductRatePlanId}
import com.gu.subscriptions.cas.model.json.ModelJsonProtocol._
import com.gu.subscriptions.cas.model.{ExpiryType, SubscriptionExpiration, SubscriptionRequest}
import com.gu.subscriptions.cas.service.api.SubscriptionService
import org.joda.time.DateTime
import org.scalatest.FreeSpec
import spray.http.HttpHeaders._
import spray.http.MediaTypes.`application/json`
import spray.http.StatusCodes.BadRequest
import spray.http.{HttpEntity, _}
import spray.json._
import spray.routing.{HttpService, Route}
import spray.testkit.ScalatestRouteTest

import scala.concurrent.Future

class ProxyDirectiveSpec extends FreeSpec with ScalatestRouteTest with ProxyDirective with HttpService {

  def actorRefFactory = system

  override implicit val actorSystem = system


  val testProbe = TestProbe()
  override lazy val io = testProbe.ref

  val handledByCAS = "Handled by CAS"
  val realCasRoute: Route = casRoute

  override lazy val casRoute: Route = complete(handledByCAS)
  val proxy = "https://example-proxy:443"
  lazy val proxyUri = Uri(proxy)
  lazy val proxyHost = proxyUri.authority.host.address
  lazy val proxyPort = proxyUri.effectivePort

  val now = new DateTime()
  val termEndDate = now.plusYears(1)
  val expiration = SubscriptionExpiration(termEndDate.plusDays(1), ExpiryType.SUB)
  val subsName = "A-S123"

  val validSubscription: Subscription = new Subscription(
      id = Subscription.Id("e4124121241234235f3245234"),
      name = Subscription.Name(subsName),
      accountId = Subscription.AccountId("a123"),
      currency = GBP,
      productRatePlanId = ProductRatePlanId("123"),
      productName = "DigitalPack",
      startDate = now.toLocalDate,
      termStartDate = now.toLocalDate,
      termEndDate = new DateTime().plusYears(1).toLocalDate,
      features = List.empty[Subscription.Feature],
      casActivationDate = None,
      isCancelled = false,
      ratePlanId = "1234",
      isPaid = true,
      promoCode = None
  )



  override lazy val subscriptionService = new SubscriptionService {

    def checkSubscriptionValidity(subscription: Subscription, postcode: String) =
      Future.successful(subscription.name.get.startsWith("A-S"))

    def getSubscription(name: String): Future[Option[Subscription]] =
      Future.successful(Some(validSubscription))

    override def updateActivationDate(subscription: Subscription): Unit = ()

    override def getValidSubscription(subscriptionName: Name, password: String) =
      Future {
        if (subscriptionName.get == subsName)
          Some(validSubscription)
        else
          None
      }

    override def isReady: Boolean = true
  }

  def inJson(r: Route): Route = respondWithMediaType(`application/json`) {r}

  "proxies the /auth requests to CAS" in {
    Post("/auth") ~> authRoute ~> check {
      assertResult(responseAs[String])(handledByCAS)
    }
  }

  "for the /subs endpoint" - {
    "when a valid request is made" - {
      "with a Zuora-formatted subscriber id" - {
        "returns the expiration with one day leeway" in {
          val payload = SubscriptionRequest(Some(subsName), Some("password")).toJson.toString()
          val req = HttpEntity(`application/json`, payload)

          Post("/subs", req) ~> inJson(subsRoute) ~> check {
            assertResult(expiration.toJson)(responseAs[String].parseJson)
          }
        }
      }

      "when an Invalid request is made" - {
        "with a Zuora-formatted subscriber id" - {
          "Returns a 404" in {
            val payload = SubscriptionRequest(Some("A-S-invalid"), Some("password")).toJson.toString()
            val req = HttpEntity(`application/json`, payload)

            Post("/subs", req) ~> inJson(subsRoute) ~> check {
              assertResult(StatusCodes.NotFound)(response.status)
            }
          }
        }
      }
      "without a Zuora format" - {

        "Drops leading zeroes before querying Zuora" in {
          val payload = SubscriptionRequest(Some("00" + subsName), Some("password")).toJson.toString()
          val req = HttpEntity(`application/json`, payload)
          Post("/subs", req) ~> inJson(subsRoute) ~> check {
            assertResult(expiration.toJson)(responseAs[String].parseJson)
          }
        }

        "proxies the request to CAS" in {
          val payload = SubscriptionRequest(Some("id"), Some("password")).toJson.toString()
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
      val proxiedRequest: HttpRequest = createProxyRequest(req, proxyUri)

      "updates the URI with proxy info" in {
        assertResult(Uri("https://example-proxy:443/endpoint"))(proxiedRequest.uri)
      }

      "updates the host header with the proxy host" in {
        assertResult(List(Host(proxyHost), fooHeader))(proxiedRequest.headers)
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

  "forwarding the HTTP request with host options" - {

    val request = new HttpRequest(HttpMethods.GET, "/")
    val hostConnectorSetup = connectorFromUrl(proxyUri)
    val msg = (createProxyRequest(request, proxyUri), hostConnectorSetup)

    "the CAS route should call IO(Http) with the expected HostConnectorSetup" - {
      proxyRequest(request, proxy, new CASMetrics("Dev"))
      testProbe.expectMsg(msg)
    }
  }

}
