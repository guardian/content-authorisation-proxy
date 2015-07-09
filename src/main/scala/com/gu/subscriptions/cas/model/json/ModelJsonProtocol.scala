package com.gu.subscriptions.cas.model.json

import com.gu.subscriptions.cas.model.SubscriptionExpiration
import spray.json._

object ModelJsonProtocol extends DefaultJsonProtocol {
  implicit object SubscriptionExpirationProtocol extends RootJsonFormat[SubscriptionExpiration] {
    override def write(sub: SubscriptionExpiration): JsValue = JsObject(
      "expiry" -> JsObject(
        "expiryType" -> JsString(sub.expiryType),
        "expiryDate" -> JsString(sub.expiryDate.toString("YYYY-MM-dd"))
    ))

    override def read(json: JsValue): SubscriptionExpiration = ??? // not needed
  }
}
