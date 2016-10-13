package com.gu.subscriptions.cas.service.api

import com.gu.memsub.Subscription.Name
import com.gu.memsub.subsv2.Subscription
import com.gu.memsub.subsv2.SubscriptionPlan.Paid

import scala.concurrent.Future

trait SubscriptionService {

  def updateActivationDate(subscription: Subscription[Paid]): Unit

  def getMatchingDigitalSubscription(subscriptionName: Name, password: String) : Future[Option[Subscription[Paid]]]

  def isReady: Boolean
}
