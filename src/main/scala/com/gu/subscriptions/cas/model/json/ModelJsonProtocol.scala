package com.gu.subscriptions.cas.model.json

import com.gu.subscriptions.CAS.CASError
import com.gu.subscriptions.cas.model.{SubscriptionRequest, SubscriptionExpiration}
import spray.json._

object ModelJsonProtocol extends DefaultJsonProtocol {
  implicit object SubscriptionExpirationProtocol extends RootJsonWriter[SubscriptionExpiration] {
    override def write(sub: SubscriptionExpiration): JsValue = JsObject(
      "expiry" -> JsObject(
        "expiryType" -> JsString(sub.expiryType),
        "expiryDate" -> JsString(sub.expiryDate.toString("YYYY-MM-dd")),
        "content" -> JsString(sub.content)
    ))
  }

  implicit val subsRequestFormat = jsonFormat2(SubscriptionRequest)

  case class CASErrorWrapper(error: CASError)

  implicit val casErrorFormat = jsonFormat2(CASError)
  implicit val casErrorWrapperFormat = jsonFormat1(CASErrorWrapper)
}
