package com.gu.subscriptions.cas.model.json

import com.gu.subscriptions.cas.model.SubscriptionExpiration
import spray.json.DefaultJsonProtocol

object ModelJsonProtocol extends DefaultJsonProtocol {
  implicit val subscriptionExpirationProtocol = jsonFormat2(SubscriptionExpiration)
}
