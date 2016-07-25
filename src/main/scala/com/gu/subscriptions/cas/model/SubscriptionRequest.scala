package com.gu.subscriptions.cas.model

case class SubscriptionRequest(subscriberId: Option[String], password: Option[String], authType: Option[String], appId: Option[String], deviceId: Option[String]) {
  lazy val hasValidCredentials = subscriberId.mkString.trim.nonEmpty && password.mkString.trim.nonEmpty
  lazy val hasValidAuth = appId.mkString.trim.nonEmpty && deviceId.mkString.trim.nonEmpty
}
