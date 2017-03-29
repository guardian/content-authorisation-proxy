package com.gu.subscriptions.cas.model.json

import com.gu.cas.SubscriptionCode
import com.gu.subscriptions.CAS.CASError
import com.gu.subscriptions.cas.model._
import spray.json._
import com.gu.subscriptions.cas.model.TokenPayloadOps._
import com.gu.subscriptions.cas.model.ExpiryType
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

  implicit object AuthorizationResponseProtocol extends RootJsonWriter[AuthResponse] {
    override def write(res: AuthResponse): JsValue = JsObject(
      "expiry" -> JsObject(
        "expiryType" -> JsString(ExpiryType.FREE),
        "provider" -> JsString("default"),
        "expiryDate" -> JsString(res.expiryDate.toString("YYYY-MM-dd"))
      )
    )
  }

  implicit val subsRequestFormat = jsonFormat3(SubscriptionRequest)
  implicit val authRequestFormat = jsonFormat2(AuthorisationRequest)

  case class CASErrorWrapper(error: CASError)

  implicit val casErrorFormat = jsonFormat2(CASError)
  implicit val casErrorWrapperFormat = jsonFormat1(CASErrorWrapper)
}
