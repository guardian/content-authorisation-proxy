package com.gu.subscriptions.cas.service

import com.gu.membership.zuora.soap.Zuora._
import org.scalatest.{Matchers, FlatSpec}

import scala.concurrent.Future

class SubscriptionServiceTest extends FlatSpec with Matchers {

  private val zuoraClient = new ZuoraClient {
    override def queryForAccount(id: String): Future[Account] = ???

    override def queryForProduct(id: String): Future[Product] = ???

    override def queryForRatePlan(subscriptionId: String): Future[RatePlan] = ???

    override def queryForProductRatePlan(id: String): Future[ProductRatePlan] = ???

    override def queryForContact(id: String): Future[Contact] = ???

    override def queryForSubscription(subscriptionId: String): Future[Subscription] = ???
  }

  val subscriptionService = new SubscriptionService(zuoraClient, List())

  behavior of "samePostcode"

  it should "ignore case of either arguments" in {
    subscriptionService.samePostcode("N1 9GU", "n1 9gu") should be(true)
    subscriptionService.samePostcode("N1 9gu", "N1 9GU") should be(true)
  }

  it should "ignore whitespaces in either arguments" in {
    subscriptionService.samePostcode("N19GU", "n1 9gu") should be(true)
    subscriptionService.samePostcode("N1 9gu", "N19GU") should be(true)
  }

  it should "not accept essentially different postcodes" in {
    subscriptionService.samePostcode("N19GU", "n2 9gu") should be(false)
    subscriptionService.samePostcode("N2 9gu", "N19GU") should be(false)
  }
}
