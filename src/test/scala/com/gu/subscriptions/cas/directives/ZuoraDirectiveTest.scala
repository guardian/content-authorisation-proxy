package com.gu.subscriptions.cas.directives


import com.gu.subscriptions.cas.model.SubscriptionRequest
import org.scalatest.{FlatSpec, Matchers}
import spray.routing.{HttpService, Route}
import spray.testkit.ScalatestRouteTest


class ZuoraDirectiveTest extends FlatSpec with Matchers with ScalatestRouteTest with HttpService {

  def actorRefFactory = system

  implicit val actorSystem = system

  "A SubscriptionRequest" should "pass if subscriberId starts with A-S" in {
    val subscriptionRequest = new SubscriptionRequest(Some("A-S00056789"), "password")
    val route: Route = ZuoraDirective.zuoraDirective(subscriptionRequest) { subId =>
      complete(subId)
    }
    Get("/") ~> route ~> check {
      responseAs[String] should be("A-S00056789")
    }
  }

  "A SubscriptionRequest" should "be rejected if the subscriberId does not start with A-S" in {
    val subscriptionRequest = new SubscriptionRequest(Some("100056789"), "password")
    val route: Route = ZuoraDirective.zuoraDirective(subscriptionRequest) { subId =>
      complete(subId)
    }
    Get("/") ~> route ~> check {
      assert(!handled)
    }
  }

}
