package com.gu.subscriptions.cas

import com.gu.membership.zuora.soap.models.Queries.Subscription

import scala.concurrent.Future

package object service {
  trait SubscriptionService {

    def updateActivationDate(subscription: Subscription): Unit

    def getValidSubscription(subscriptionName: String, password: String) : Future[Option[Subscription]]

    def isReady: Boolean
  }
}
