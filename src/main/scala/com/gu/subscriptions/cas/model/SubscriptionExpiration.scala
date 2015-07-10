package com.gu.subscriptions.cas.model
import org.joda.time.DateTime

case class SubscriptionExpiration(expiryDate: DateTime, expiryType: String = "subs")
