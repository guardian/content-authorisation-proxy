package com.gu.subscriptions.cas.model
import com.gu.cas.SubscriptionCode
import org.joda.time.DateTime

case class SubscriptionExpiration(expiryDate: DateTime, expiryType: String, content: String = "SevenDay", subscriptionCode: Option[SubscriptionCode] = None, provider: Option[String] = None)

trait AuthorizationResponse
case class AuthExpiryResponse(expiryDate: DateTime) extends AuthorizationResponse
case class ErrorResponse(message: String, code: Int) extends AuthorizationResponse

object ExpiryType {
  val SUB = "sub"
  val FREE = "free"
  val SPECIAL = "special" // deprecated
  val DEFAULT = "default"
  val DEVICE_CONFIGURED= "deviceConfigured"
}
