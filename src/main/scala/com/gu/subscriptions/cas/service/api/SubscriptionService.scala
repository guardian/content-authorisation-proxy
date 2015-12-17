package com.gu.subscriptions.cas.service.api

import com.gu.zuora.soap.models.{Queries => SoapQueries}

import scala.concurrent.Future

trait SubscriptionService {

  def updateActivationDate(subscription: SoapQueries.Subscription): Unit

  def getValidSubscription(subscriptionName: String, password: String) : Future[Option[SoapQueries.Subscription]]

  def isReady: Boolean
}
