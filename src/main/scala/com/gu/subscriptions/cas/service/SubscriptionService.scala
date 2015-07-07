package com.gu.subscriptions.cas.service

import com.gu.membership.zuora.ZuoraApiConfig
import com.gu.membership.zuora.soap.Zuora.{Authentication, RatePlan}
import com.gu.membership.zuora.soap._
import com.gu.monitoring.ZuoraMetrics
import com.gu.subscriptions.cas.config.Configuration
import com.gu.subscriptions.cas.model.SubscriptionExpiration
import com.gu.subscriptions.cas.service.utils.ScheduledTask

import scala.concurrent.Future
import scala.concurrent.duration._

class SubscriptionService(zuoraClient: ZuoraClient) {
  import spray.json._
  import DefaultJsonProtocol._

  case class SubsRequest(subscriberId: Option[String])
  implicit val subsRequestFormat = jsonFormat1(SubsRequest)

  def extractZuoraSubscriptionId(requestJson: String): Option[String] =
    requestJson.parseJson.convertTo[SubsRequest].subscriberId.filter(_.startsWith("A-S"))

  def verifySubscriptionExpiration(subscriptionId:String): SubscriptionExpiration = {
    SubscriptionExpiration("A", "B")
  }
}

object SubscriptionService extends SubscriptionService(ZuoraClient)

trait ZuoraClient {
  def retrieveRatePlan(subscriptionId:String): Future[Zuora.RatePlan]
}

object ZuoraClient extends ZuoraApi with ZuoraClient {

  import ZuoraDeserializer._

  override val apiConfig: ZuoraApiConfig = ZuoraApiConfig.from(Configuration.zuoraConfig, Configuration.stage)

  override implicit def authentication: Authentication = authTask.get()

  override val application: String = Configuration.appName
  override val stage: String = Configuration.stage

  override val metrics = new ZuoraMetrics(stage, application)
  
  lazy val authTask = ScheduledTask(s"Zuora ${apiConfig.envName} auth", Authentication("", ""), 0.seconds, 30.minutes)(
    request(Login(apiConfig)))

  def retrieveRatePlan(subscriptionId:String): Future[Zuora.RatePlan] =
    queryOne[RatePlan](s"SubscriptionId='$subscriptionId'")
}