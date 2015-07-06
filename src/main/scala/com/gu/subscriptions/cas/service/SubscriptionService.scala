package com.gu.subscriptions.cas.service

import com.gu.subscriptions.cas.model.SubscriptionExpiration

object SubscriptionService {

  import spray.json._
  import DefaultJsonProtocol._

  case class SubsRequest(subscriberId: Option[String])
  implicit val subsRequestFormat = jsonFormat1(SubsRequest)

  def extractZuoraSubscriptionId(requestJson: String): Option[String] =
    requestJson.parseJson.convertTo[SubsRequest].subscriberId.filter(_.startsWith("A-S"))

  def verifySubscriptionExpiration(subscriptionId:String): SubscriptionExpiration = SubscriptionExpiration("A", "B")
}
