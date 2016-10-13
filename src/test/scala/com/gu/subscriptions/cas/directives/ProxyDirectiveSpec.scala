package com.gu.subscriptions.cas.directives

import akka.testkit.TestProbe
import com.gu.i18n.GBP
import com.gu.memsub.Subscription.{Name, ProductRatePlanId, RatePlanId}
import com.gu.memsub._
import com.gu.memsub.subsv2.SubscriptionPlan.{Digipack, Paid, Paper}
import com.gu.memsub.subsv2.{PaidCharge, PaidSubscriptionPlan, PaperCharges}
import com.gu.subscriptions.cas.config.Configuration._
import com.gu.subscriptions.cas.model.json.ModelJsonProtocol._
import com.gu.subscriptions.cas.model.{ExpiryType, SubscriptionExpiration, SubscriptionRequest}
import com.gu.subscriptions.cas.service.api.SubscriptionService
import org.joda.time.DateTime
import org.scalatest.FreeSpec
import spray.http.HttpHeaders._
import spray.http.MediaTypes.`application/json`
import spray.http.StatusCodes.BadRequest
import spray.http.{HttpEntity, _}
import spray.json.{JsNumber, JsString, _}
import spray.routing.{HttpService, Route}
import spray.testkit.ScalatestRouteTest

import scala.collection.immutable.List.empty
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

  val now = DateTime.now
  val today = now.toLocalDate
  val termEndDate = now.plusYears(1)
  val expiration = SubscriptionExpiration(termEndDate.plusDays(1), ExpiryType.SUB)
  val cancelled = JsObject("error" -> JsObject("message" -> JsString("Subscription inactive (Cancelled)"), "code" -> JsNumber(subscriptionDisabledErrorCode)))
  val subsName1 = "A-S123"
  val subsName2 = "A-S456"
  val subsName3 = "A-S789"

  private val digipackSubscriptionPlan: Digipack = PaidSubscriptionPlan[Product.ZDigipack, PaidCharge[Digipack.type, BillingPeriod]](
    id = RatePlanId("1234"),
    name = "DigitalPack",
    productRatePlanId = ProductRatePlanId("123"),
    description = "",
    productName = "Digital Pack",
    product = Product.Digipack,
    features = empty,
    charges = PaidCharge(Digipack, BillingPeriod.year, PricingSummary(Map(GBP -> Price(119.90f, GBP)))),
    chargedThrough = Some(today),
    start = today,
    end = today.plusYears(1)
  )

  private val plusPaperPackageSubscriptionPlan: Paper = PaidSubscriptionPlan[Product.Voucher, PaperCharges](
    id = RatePlanId("5678"),
    name = "Everyday+",
    productRatePlanId = ProductRatePlanId("456"),
    description = "",
    productName = "Paper Voucher",
    product = Product.Voucher,
    features = empty,
    charges = PaperCharges(
      dayPrices = Map(SundayPaper -> PricingSummary(Map(GBP -> Price(3.0f, GBP)))),
      digipack = Some(PricingSummary(Map(GBP -> Price(119.90f, GBP))))),
    chargedThrough = Some(today),
    start = today,
    end = today.plusYears(1)
  )

  private val digitalSubscription1 = new com.gu.memsub.subsv2.Subscription[Paid](
    id = Subscription.Id("e4124121241234235f3245234"),
    name = Subscription.Name(subsName1),
    accountId = Subscription.AccountId("a123"),
    startDate = today,
    firstPaymentDate = today,
    termStartDate = today,
    termEndDate = today.plusYears(1),
    casActivationDate = None,
    promoCode = None,
    isCancelled = false,
    hasPendingFreePlan = false,
    plan = digipackSubscriptionPlan
  )

  private val digitalSubscription2 = digitalSubscription1.copy(name = Subscription.Name(subsName2), plan = plusPaperPackageSubscriptionPlan)

  private val digitalSubscription3 = digitalSubscription1.copy(name = Subscription.Name(subsName3), isCancelled = true)

  private val subsByName = Map(
    subsName1 -> digitalSubscription1,
    subsName2 -> digitalSubscription2,
    subsName3 -> digitalSubscription3
  )

  override lazy val subscriptionService = new SubscriptionService {

    override def updateActivationDate(subscription: subsv2.Subscription[Paid]): Unit = ()

    override def getMatchingDigitalSubscription(subscriptionName: Name, password: String): Future[Option[com.gu.memsub.subsv2.Subscription[Paid]]] =
      Future {
        subsByName.get(subscriptionName.get).filter(_.plan.charges.benefits.list.contains(Digipack))
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
        "for a digipack subscription" - {
          "returns the expiration with one day leeway" in {
            val payload = SubscriptionRequest(Some(subsName1), Some("password")).toJson.toString()
            val req = HttpEntity(`application/json`, payload)

            Post("/subs", req) ~> inJson(subsRoute) ~> check {
              assertResult(expiration.toJson)(responseAs[String].parseJson)
            }
          }
        }
        "for a Newspaper 'plus' package subscription" - {
          "returns the expiration with one day leeway" in {
            val payload = SubscriptionRequest(Some(subsName2), Some("password")).toJson.toString()
            val req = HttpEntity(`application/json`, payload)

            Post("/subs", req) ~> inJson(subsRoute) ~> check {
              assertResult(expiration.toJson)(responseAs[String].parseJson)
            }
          }
        }
        "for an expired digipack subscription" - {
          "returns the expiration with one day leeway" in {
            val payload = SubscriptionRequest(Some(subsName3), Some("password")).toJson.toString()
            val req = HttpEntity(`application/json`, payload)

            Post("/subs", req) ~> inJson(subsRoute) ~> check {
              assertResult(cancelled.toJson)(responseAs[String].parseJson)
            }
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
          val payload = SubscriptionRequest(Some("00" + subsName1), Some("password")).toJson.toString()
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
