package com.gu.subscriptions.cas.service

import java.util.concurrent.{Callable, FutureTask}

import com.amazonaws.regions.{AwsFakes, Region}
import com.amazonaws.services.cloudwatch.model.Dimension
import com.gu.membership.zuora.soap.Zuora._
import com.gu.monitoring.CloudWatch
import com.gu.subscriptions.cas.service.zuora.{ZuoraSubscriptionService, ZuoraClient, ZuoraSubscriptionService$$}
import org.scalatest.{Matchers, FlatSpec}

import scala.concurrent.Future

class ZuoraSubscriptionServiceTest extends FlatSpec with Matchers {

  private val zuoraClient = new ZuoraClient {
    override def queryForAccount(id: String): Future[Account] = ???
    override def queryForProduct(id: String): Future[Product] = ???
    override def queryForRatePlan(subscriptionId: String): Future[RatePlan] = ???
    override def queryForProductRatePlan(id: String): Future[ProductRatePlan] = ???
    override def queryForContact(id: String): Future[Contact] = ???
    override def queryForSubscription(subscriptionId: String): Future[Subscription] = ???
  }

  private val cloudWatch = new CloudWatch {
    override val application: String = "test"
    override val region: Region = AwsFakes.FakeRegion
    override val service: String = "test"
    override val stage: String = "test"
  }

  val subscriptionService = new ZuoraSubscriptionService(zuoraClient, List(), cloudWatch)

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
