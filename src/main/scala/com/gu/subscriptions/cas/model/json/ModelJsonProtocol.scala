package com.gu.subscriptions.cas.model.json

import com.gu.cas.SubscriptionCode
import com.gu.subscriptions.CAS.CASError
import com.gu.subscriptions.cas.model.{AuthorisationRequest, SubscriptionExpiration, SubscriptionRequest}
import spray.json._
import com.gu.subscriptions.cas.model.TokenPayloadOps._

object ModelJsonProtocol extends DefaultJsonProtocol {
  implicit object SubscriptionExpirationProtocol extends RootJsonWriter[SubscriptionExpiration] {
    override def write(sub: SubscriptionExpiration): JsValue = {
      val values = Map(
          "expiryType" -> JsString(sub.expiryType),
          "expiryDate" -> JsString(sub.expiryDate.toString("YYYY-MM-dd")),
          "content" -> JsString(sub.content)
      )
      val optionalValues = Seq(
        sub.provider.map(p => "provider" -> JsString(p)),
        sub.subscriptionCode.map(c => "subscriptionCode" -> JsString(c.asString))
        ).flatten
      JsObject(
        "expiry" -> JsObject(values ++ optionalValues)
      )
    }
  }

  implicit val subsRequestFormat = jsonFormat3(SubscriptionRequest)
  implicit val authRequestFormat = jsonFormat3(AuthorisationRequest)

  case class CASErrorWrapper(error: CASError)

  implicit val casErrorFormat = jsonFormat2(CASError)
  implicit val casErrorWrapperFormat = jsonFormat1(CASErrorWrapper)
}
