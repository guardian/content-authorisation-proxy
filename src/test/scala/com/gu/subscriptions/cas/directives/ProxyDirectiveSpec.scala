package com.gu.subscriptions.cas.directives

import com.gu.subscriptions.cas.model.json.ModelJsonProtocol._
import com.gu.subscriptions.cas.model.{SubscriptionExpiration, SubscriptionRequest}
import com.gu.subscriptions.cas.service.SubscriptionService
import org.joda.time.DateTime
import org.scalatest.FreeSpec
import spray.http.HttpEntity
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
  val expiration = SubscriptionExpiration(DateTime.now())

  override lazy val casRoute: Route = complete(handledByCAS)

  override lazy val subscriptionService = new SubscriptionService {
    override def verifySubscriptionExpiration(subscriptionName: String, postcode: String) =
      Future { expiration }
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
}
