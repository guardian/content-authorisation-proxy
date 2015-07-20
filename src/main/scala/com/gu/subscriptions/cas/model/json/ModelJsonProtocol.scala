package com.gu.subscriptions.cas.model.json

import com.gu.cas.CAS.CASError
import com.gu.subscriptions.cas.model.{SubscriptionRequest, SubscriptionExpiration}
import spray.json._

object ModelJsonProtocol extends DefaultJsonProtocol {
  implicit object SubscriptionExpirationProtocol extends RootJsonWriter[SubscriptionExpiration] {
    override def write(sub: SubscriptionExpiration): JsValue = JsObject(
      "expiry" -> JsObject(
        "expiryType" -> JsString(sub.expiryType),
        "expiryDate" -> JsString(sub.expiryDate.toString("YYYY-MM-dd"))
    ))
  }

  implicit val subsRequestFormat = jsonFormat2(SubscriptionRequest)

  case class CASErrorInternal(message: String, code: Int)

  implicit val casErrorFormat = jsonFormat2(CASErrorInternal)

  implicit object CASErrorProtocol extends RootJsonFormat[CASError] {
    override def write(error: CASError): JsValue = JsObject(
      "error" -> CASErrorInternal(error.message, error.code).toJson
    )

    //In the CASError definition, deserialization code uses play.json
    override def read(json: JsValue): CASError = json match {
      case JsObject(map) =>
        map.get("error") match {
          case Some(obj) =>
            val error = obj.convertTo[CASErrorInternal]
            CASError(error.message, error.code)
          case None =>
            deserializationError("Expected an error field")
        }

      case _ => deserializationError("Expected a json object")
    }
  }
}
