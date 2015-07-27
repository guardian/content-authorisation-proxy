package com.gu.subscriptions.cas

import com.gu.subscriptions.cas.model.SubscriptionExpiration

import scala.concurrent.Future

package object service {
  trait SubscriptionService {
    def verifySubscriptionExpiration(subscriptionName: String, postcode: String): Future[Option[SubscriptionExpiration]]
  }
}
