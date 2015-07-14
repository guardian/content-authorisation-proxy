package com.gu.subscriptions.cas.service

import com.gu.membership.zuora.ZuoraApiConfig
import com.gu.membership.zuora.soap.Zuora._
import com.gu.membership.zuora.soap._
import com.gu.monitoring.ZuoraMetrics
import com.gu.subscriptions.cas.config.Configuration
import com.gu.subscriptions.cas.model.SubscriptionExpiration
import com.gu.subscriptions.cas.service.utils.ScheduledTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class SubscriptionService(zuoraClient: ZuoraClient) {
  import spray.json._
  import DefaultJsonProtocol._

  case class SubsRequest(subscriberId: Option[String], password:String)
  implicit val subsRequestFormat = jsonFormat2(SubsRequest)

  def extractZuoraSubscriptionId(subsRequest: SubsRequest): Option[String] =
    subsRequest.subscriberId.filter(_.startsWith("A-S"))

  def extractSubsRequest(requestJson: String): SubsRequest = {
    requestJson.parseJson.convertTo[SubsRequest]
  }

  val productId = "2c92c0f84b786da2014b91d3629b4298"

  def verifySubscriptionExpiration(subscriptionName: String, postcode: String): Future[SubscriptionExpiration] =
    for {
      subscription <- zuoraClient.queryForSubscription(subscriptionName)

      ratePlan <- zuoraClient.queryForRatePlan(subscription.id)
      productRatePlan <- zuoraClient.queryForProductRatePlan(ratePlan.productRatePlanId)
      product <- zuoraClient.queryForProduct(productRatePlan.productId)
        if product.name == "Digital Pack"

      account <- zuoraClient.queryForAccount(subscription.accountId)
      contact <- zuoraClient.queryForContact(account.billToId)
        if contact.postalCode == postcode

    } yield {
      println(productRatePlan)
      println(product)
      println(contact)
      SubscriptionExpiration(subscription.termEndDate)
    }
}

object SubscriptionService extends SubscriptionService(ZuoraClient)

trait ZuoraClient {
  def queryForSubscription(subscriptionId:String): Future[Zuora.Subscription]
  def queryForRatePlan(subscriptionId: String): Future[Zuora.RatePlan]
  def queryForProductRatePlan(id: String): Future[Zuora.ProductRatePlan]
  def queryForAccount(id: String): Future[Zuora.Account]
  def queryForContact(id: String): Future[Zuora.Contact]
  def queryForProduct(id: String): Future[Zuora.Product]
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

  def queryForSubscription(subscriptionId: String): Future[Zuora.Subscription] =
    queryOne[Zuora.Subscription](s"Name='$subscriptionId'")

  def queryForProduct(id: String): Future[Zuora.Product] =
    queryOne[Zuora.Product](s"Id='$id'")

  def queryForRatePlan(subscriptionId: String): Future[Zuora.RatePlan] =
    queryOne[Zuora.RatePlan](s"SubscriptionId='$subscriptionId'")

  def queryForProductRatePlan(id: String): Future[Zuora.ProductRatePlan] =
    queryOne[Zuora.ProductRatePlan](s"Id='$id'")

  def queryForAccount(id: String): Future[Zuora.Account] =
    queryOne[Zuora.Account](s"Id='$id'")

  def queryForContact(id: String): Future[Zuora.Contact] =
    queryOne[Zuora.Contact](s"Id='$id'")

}