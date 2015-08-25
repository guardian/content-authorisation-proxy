package com.gu.subscriptions.cas

import com.gu.membership.zuora.soap.Zuora.Subscription
import com.gu.subscriptions.cas.model.SubscriptionExpiration

import scala.concurrent.Future

package object service {
  trait SubscriptionService {
    def updateActivationDate(subscription: Subscription): Unit

    def verifySubscriptionExpiration(subscription: Subscription, postcode: String): Future[Option[SubscriptionExpiration]]

    def getSubscription(name: String): Future[Option[Subscription]]
  }
}
