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

class SubscriptionService(zuoraClient: ZuoraClient, knownProducts: List[String]) {
  import spray.json._
  import DefaultJsonProtocol._

  case class SubsRequest(subscriberId: Option[String], password:String)
  implicit val subsRequestFormat = jsonFormat2(SubsRequest)

  def extractZuoraSubscriptionId(subsRequest: SubsRequest): Option[String] =
    subsRequest.subscriberId.filter(_.startsWith("A-S"))

  def extractSubsRequest(requestJson: String): SubsRequest = {
    requestJson.parseJson.convertTo[SubsRequest]
  }

  val samePostcode: (String, String) => Boolean = {
    val format: String => String = _.replaceAll("\\s+", "").toLowerCase

    (postcodeA, postcodeB) => format(postcodeA) == format(postcodeB)
  }

  def verifySubscriptionExpiration(subscriptionName: String, postcode: String): Future[SubscriptionExpiration] =
    zuoraClient.queryForSubscription(subscriptionName).flatMap { subscription=>
      val knownProductCheck = for {
        ratePlan <- zuoraClient.queryForRatePlan(subscription.id)
        productRatePlan <- zuoraClient.queryForProductRatePlan(ratePlan.productRatePlanId)
        product <- zuoraClient.queryForProduct(productRatePlan.productId)
      } yield knownProducts.contains(product.name)

      val postcodeCheck = for {
        account <- zuoraClient.queryForAccount(subscription.accountId)
        contact <- zuoraClient.queryForContact(account.billToId)
      } yield samePostcode(contact.postalCode, postcode)

      for {
        productsMatch <- knownProductCheck if productsMatch
        postcodesMatch <- postcodeCheck if postcodesMatch
      } yield SubscriptionExpiration(subscription.termEndDate)
    }
}

object SubscriptionService extends SubscriptionService(ZuoraClient, Configuration.knownProducts)

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