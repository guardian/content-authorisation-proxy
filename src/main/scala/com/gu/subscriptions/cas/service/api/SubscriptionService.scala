package com.gu.subscriptions.cas.service.api

import com.gu.memsub.Subscription
import com.gu.memsub.Subscription.Name

import scala.concurrent.Future

trait SubscriptionService {

  def updateActivationDate(subscription: Subscription): Unit

  def getValidSubscription(subscriptionName: Name, password: String) : Future[Option[Subscription]]

  def isReady: Boolean
}
