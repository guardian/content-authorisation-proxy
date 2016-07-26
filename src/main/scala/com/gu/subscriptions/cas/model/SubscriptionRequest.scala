package com.gu.subscriptions.cas.model

case class SubscriptionRequest(subscriberId: Option[String], password: Option[String], authType: Option[String])