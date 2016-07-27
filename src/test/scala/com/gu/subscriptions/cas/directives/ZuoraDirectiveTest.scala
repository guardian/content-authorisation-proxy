package com.gu.subscriptions.cas.directives


import com.gu.subscriptions.cas.model.SubscriptionRequest
import org.scalatest.{FlatSpec, Matchers}
import spray.http.Uri
import spray.routing.{HttpService, Route}
import spray.testkit.ScalatestRouteTest


class ZuoraDirectiveTest extends FlatSpec with Matchers with ScalatestRouteTest with HttpService {

  def actorRefFactory = system

  implicit val actorSystem = system

  def routeFor(s: SubscriptionRequest): Route =
    ZuoraDirective.zuoraDirective(s) { (triggersActivation, subId) => complete(subId.get)}

  "A SubscriptionRequest" should "pass always" in {

    // we always want to check zuora now people are being migrated over
    val zuoraSubRequest = new SubscriptionRequest(Some("A-S00056789"), Some("password"))
    val casSubRequest = new SubscriptionRequest(Some("00123455"), Some("password"))
    Get("/") ~> routeFor(zuoraSubRequest) ~> check { responseAs[String] should be("A-S00056789") }
    Get("/") ~> routeFor(casSubRequest) ~> check { responseAs[String] should be("00123455") }
  }


  "A SubscriptionRequest" should "return a true triggersActivation value unless given a noActivation query parameter" in {
    val subscriptionRequest = new SubscriptionRequest(Some("A-S00056789"), Some("password"))
    val route: Route = ZuoraDirective.zuoraDirective(subscriptionRequest) { (triggersActivation, subId) =>
      val activation = if (triggersActivation) "true" else "false"
      complete(activation)
    }

    Get("/") ~> route ~> check {
      responseAs[String] should be("true")
    }

    Get(Uri("/").withQuery(("noActivation", "true"))) ~> route ~> check {
      responseAs[String] should be("false")
    }
  }

}
