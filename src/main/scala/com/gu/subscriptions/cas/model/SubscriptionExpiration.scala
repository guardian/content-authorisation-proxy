package com.gu.subscriptions.cas.model
import org.joda.time.DateTime

case class SubscriptionExpiration(expiryDate: DateTime, expiryType: String, content: String = "SevenDay")

object ExpiryType {
  val SUB = "sub"
  val FREE = "free"
  val SPECIAL = "special" // deprecated
  val DEFAULT = "default"
  val DEVICE_CONFIGURED= "deviceConfigured"
}
