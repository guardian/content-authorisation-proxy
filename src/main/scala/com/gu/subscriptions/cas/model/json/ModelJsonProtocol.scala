package com.gu.subscriptions.cas.model.json

import com.gu.cas.SubscriptionCode
import com.gu.subscriptions.CAS.CASError
import com.gu.subscriptions.cas.model.{AuthorisationRequest, LegacySubscriptionExpiration, SubscriptionExpiration, SubscriptionRequest}
import spray.json._
import com.gu.subscriptions.cas.model.TokenPayloadOps._

object ModelJsonProtocol extends DefaultJsonProtocol {
  implicit object SubscriptionExpirationProtocol extends RootJsonWriter[SubscriptionExpiration] {
    override def write(sub: SubscriptionExpiration): JsValue = JsObject(
      "expiry" -> JsObject(
        "expiryType" -> JsString(sub.expiryType),
        "expiryDate" -> JsString(sub.expiryDate.toString("YYYY-MM-dd")),
        "content" -> JsString(sub.content)
    ))
  }
  //TODO MAYBE UNIFY THIS WITH SUBSCRIPTION EXPIRATION?
  implicit object LegacySubscriptionExpirationProtocol extends RootJsonWriter[LegacySubscriptionExpiration] {
    override def write(sub: LegacySubscriptionExpiration): JsValue = JsObject(
      "expiry" -> JsObject(
        "expiryType" -> JsString(sub.expiryType),
        "provider" -> JsString(sub.provider),
        "expiryDate" -> JsString(sub.expiryDate.toString("YYYY-MM-dd")),
        "content" -> JsString(sub.content),
        "subscriptionCode" ->JsString(sub.subscriptionCode.asString)
      ))
  }


    implicit val subsRequestFormat = jsonFormat3(SubscriptionRequest)
  implicit val authRequestFormat = jsonFormat3(AuthorisationRequest)

  case class CASErrorWrapper(error: CASError)

  implicit val casErrorFormat = jsonFormat2(CASError)
  implicit val casErrorWrapperFormat = jsonFormat1(CASErrorWrapper)
}
