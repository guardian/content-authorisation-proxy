package com.gu.subscriptions.cas.model
import com.gu.cas.SubscriptionCode
import org.joda.time.{DateTime, LocalDate}

case class SubscriptionExpiration(expiryDate: DateTime, expiryType: String, content: String = "SevenDay", subscriptionCode: Option[SubscriptionCode] = None, provider: Option[String] = None)

case class AuthResponse(expiryDate: LocalDate)

object ExpiryType {
  val SUB = "sub"
  val FREE = "free"
  val SPECIAL = "special" // deprecated
  val DEFAULT = "default"
  val DEVICE_CONFIGURED= "deviceConfigured"
}
