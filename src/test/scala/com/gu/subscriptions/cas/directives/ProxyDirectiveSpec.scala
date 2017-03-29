package com.gu.subscriptions.cas.directives

import com.gu.cas._
import com.gu.i18n.Currency.GBP
import com.gu.memsub.Subscription.{Name, ProductRatePlanChargeId, ProductRatePlanId, RatePlanId}
import com.gu.memsub._
import com.gu.memsub.Benefit._
import com.gu.memsub.subsv2.SubscriptionPlan.{Digipack, Paid}
import com.gu.memsub.subsv2.{PaidCharge, PaidSubscriptionPlan, PaperCharges, ReaderType}
import com.gu.subscriptions.cas.model.json.ModelJsonProtocol._
import com.gu.subscriptions.cas.model._
import com.gu.subscriptions.cas.service.api._
import org.joda.time.{DateTime, LocalDate, ReadablePeriod, Weeks, Days}
import org.scalatest.FreeSpec
import spray.http.MediaTypes.`application/json`
import spray.http.StatusCodes.BadRequest
import spray.http.{HttpEntity, _}
import spray.json._
import spray.routing.{HttpService, Route}
import spray.testkit.ScalatestRouteTest

import scala.concurrent.Future
import scalaz.NonEmptyList



class ProxyDirectiveSpec extends FreeSpec with ScalatestRouteTest with ProxyDirective with HttpService {

  object testDataStore extends DataStore {
    override def getExpiration(appId: String, deviceId: String): Future[GetExpirationResponse] = (appId, deviceId) match {
      case ("existingApp", "existingDevice") => Future.successful(Success(Some(aWeekFromNow)))
      case _ => Future.successful(Success(None))
    }
    override def setExpiration(appId: String, deviceId: String, expiration: LocalDate, timeToLive: ReadablePeriod): Future[SetExpirationResponse] = Future.successful(Success)
  }

  def actorRefFactory = system

  override implicit val actorSystem = system
    override val dataStore = testDataStore
    val now = DateTime.now
    val today = now.toLocalDate
    val termEndDate = now.plusYears(1)
    val aWeekFromNow = now.plusWeeks(1).toLocalDate
    val twoWeeksFromNow = now.plusWeeks(2).toLocalDate
    val expiration = SubscriptionExpiration(termEndDate.plusDays(1), ExpiryType.SUB)
    val emergencyExpiration = SubscriptionExpiration(
      expiryDate = DateTime.parse("2018-03-14"),
      expiryType = ExpiryType.SUB,
      subscriptionCode = Some(SevenDay),
      provider = Some("G99")
    )
    val subsName = "A-S123"

    private val digipackSubscriptionPlan: Digipack = PaidSubscriptionPlan[Product.ZDigipack, PaidCharge[Digipack.type, BillingPeriod]](
      id = RatePlanId("1234"),
      name = "DigitalPack",
      productRatePlanId = ProductRatePlanId("123"),
      description = "",
      productName = "Digital Pack",
      product = Product.Digipack,
      features = List.empty,
      charges = PaidCharge(Digipack, BillingPeriod.Year, PricingSummary(Map(GBP -> Price(119.90f, GBP))), ProductRatePlanChargeId("foo")),
      chargedThrough = Some(today),
      start = today,
      end = today.plusYears(1)
    )

    private val plusPaperPackageSubscriptionPlan = PaidSubscriptionPlan[Product.Voucher, PaperCharges](
      id = RatePlanId("5678"),
      name = "Everyday+",
      productRatePlanId = ProductRatePlanId("456"),
      description = "",
      productName = "Paper Voucher",
      product = Product.Voucher,
      features = List.empty,
      charges = PaperCharges(
        dayPrices = Map(SundayPaper -> PricingSummary(Map(GBP -> Price(3.0f, GBP)))),
        digipack = Some(PricingSummary(Map(GBP -> Price(119.90f, GBP))))),
      chargedThrough = Some(today),
      start = today,
      end = today.plusYears(1)
    )

    private val validSubscription1 = new com.gu.memsub.subsv2.Subscription[Paid](
      id = Subscription.Id("e4124121241234235f3245234"),
      name = Subscription.Name(subsName),
      accountId = Subscription.AccountId("a123"),
      startDate = today,
      acceptanceDate = today.plusDays(14), // 14 days free trial
      termStartDate = today,
      termEndDate = today.plusYears(1),
      casActivationDate = None,
      promoCode = None,
      isCancelled = false,
      hasPendingFreePlan = false,
      plans = NonEmptyList(digipackSubscriptionPlan),
      readerType = ReaderType.Direct,
      autoRenew = true
    )

    private val validSubscription2 = validSubscription1.copy(plans = NonEmptyList(plusPaperPackageSubscriptionPlan))


    object testDecoder extends PrefixedTokens("test", "G99") {
      override def decode(subId: String) = if (subId == "G99TESTID")
        Valid(TokenPayload(
          creationDateOffset = Days.days(1636),
          period = Weeks.weeks(52),
          subscriptionCode = SevenDay))

      else
        Invalid(None)

    }

    object testEmergencyTokens extends EmergencyTokens("G99", "test") {
      override val decoder = testDecoder
    }

    override val emergencyTokens = testEmergencyTokens
    override lazy val subscriptionService = new SubscriptionService {

      override def updateActivationDate(subscription: subsv2.Subscription[Paid]): Unit = ()

      override def getValidSubscription(subscriptionName: Name, password: String) =
        Future {
          if (subscriptionName.get == subsName &&
            validSubscription1.plan.charges.benefits.list.contains(Digipack) &&
            validSubscription2.plan.charges.benefits.list.contains(Digipack)
          )
            Some(validSubscription2)
          else
            None
        }

      override def isReady: Boolean = true
    }

    def inJson(r: Route): Route = respondWithMediaType(`application/json`) {
      r
    }

  "for the /auth endpoint" - {
    "with an existing device and app id combination" - {
      "returns expiration date from data store" in {
        val payload = AuthorisationRequest(Some("existingApp"), Some("existingDevice")).toJson.toString()
        val req = HttpEntity(`application/json`, payload)
        Post("/auth", req) ~> inJson(authRoute) ~> check {
          assertResult(AuthResponse(aWeekFromNow).toJson)(responseAs[String].parseJson)
        }
      }
    }
    "with a new device and app id combination" - {
      "sets expiration date two weeks in the future" in {
        val payload = AuthorisationRequest(Some("newApp"), Some("newDevice")).toJson.toString()
        val req = HttpEntity(`application/json`, payload)
        Post("/auth", req) ~> inJson(authRoute) ~> check {
          assertResult(AuthResponse(twoWeeksFromNow).toJson)(responseAs[String].parseJson)
        }
      }
    }
  }

  "for the /subs endpoint" - {
    "when a valid request is made" - {
      "with a Zuora-formatted subscriber id" - {
        "returns the expiration with one day leeway" in {
          val payload = SubscriptionRequest(Some(subsName), Some("password"), None).toJson.toString()
          val req = HttpEntity(`application/json`, payload)

          Post("/subs", req) ~> inJson(subsRoute) ~> check {
            assertResult(expiration.toJson)(responseAs[String].parseJson)
          }
        }
      }

      "when an Invalid request is made" - {
        "with a Zuora-formatted subscriber id" - {
          "Returns a 404" in {
            val payload = SubscriptionRequest(Some("A-S-invalid"), Some("password"), None).toJson.toString()
            val req = HttpEntity(`application/json`, payload)

            Post("/subs", req) ~> inJson(subsRoute) ~> check {
              assertResult(StatusCodes.NotFound)(response.status)
            }
          }
        }
      }
      "without a Zuora format" - {

        "Drops leading zeroes before querying Zuora" in {
          val payload = SubscriptionRequest(Some("00" + subsName), Some("password"), None).toJson.toString()
          val req = HttpEntity(`application/json`, payload)
          Post("/subs", req) ~> inJson(subsRoute) ~> check {
            assertResult(expiration.toJson)(responseAs[String].parseJson)
          }

        }

        "handles emergency tokens" in {
          val payload = SubscriptionRequest(Some("G99TESTID"), Some("password"), None).toJson.toString()
          val req = HttpEntity(`application/json`, payload)

          Post("/subs", req) ~> inJson(subsRoute) ~> check {
            assertResult(emergencyExpiration.toJson)(responseAs[String].parseJson)
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


}
