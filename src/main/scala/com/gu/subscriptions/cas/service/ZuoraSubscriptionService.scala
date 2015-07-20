package com.gu.subscriptions.cas.service

import com.amazonaws.regions.{Region, Regions}
import com.gu.membership.zuora.soap.Zuora._
import com.gu.membership.zuora.soap._
import com.gu.monitoring.{CloudWatch, ZuoraMetrics}
import com.gu.subscriptions.cas.config.Configuration
import com.gu.subscriptions.cas.model.SubscriptionExpiration
import com.gu.subscriptions.cas.service.utils.ScheduledTask
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class ZuoraSubscriptionService(zuoraClient: ZuoraClient,
                          knownProducts: List[String],
                          cloudWatch: CloudWatch) extends LazyLogging with SubscriptionService {

  val samePostcode: (String, String) => Boolean = {
    val format: String => String = _.replaceAll("\\s+", "").toLowerCase

    (postcodeA, postcodeB) => format(postcodeA) == format(postcodeB)
  }

  private def knownProductCheck(subscription: Subscription): Future[Boolean] =
    for {
      ratePlan <- zuoraClient.queryForRatePlan(subscription.id)
      productRatePlan <- zuoraClient.queryForProductRatePlan(ratePlan.productRatePlanId)
      product <- zuoraClient.queryForProduct(productRatePlan.productId)
    } yield knownProducts.contains(product.name)

  private def postcodeCheck(subscription: Subscription, postcode: String): Future[Boolean] =
    for {
      account <- zuoraClient.queryForAccount(subscription.accountId)
      contact <- zuoraClient.queryForContact(account.billToId)
    } yield {
      val postcodesMatch = samePostcode(contact.postalCode, postcode)
      if (!postcodesMatch) {
        cloudWatch.put("Postcodes not matching", 1)
        logger.info(s"Postcodes not matching: ${contact.postalCode}, $postcode")
      }
      postcodesMatch
    }

  /**
   * @return Some(Subscription) if the lookup was successful, None if the query
   *         an empty result set.
   */
  override def verifySubscriptionExpiration(subscriptionName: String, postcode: String): Future[Option[SubscriptionExpiration]] =
    (for {
      subscription <- zuoraClient.queryForSubscription(subscriptionName)
      productsMatch <- knownProductCheck(subscription)
      postcodesMatch <- postcodeCheck(subscription, postcode)
    } yield {
      if (productsMatch && postcodesMatch)
        Some(SubscriptionExpiration(subscription.termEndDate))
      else
        None
    }) recover { case e: ZuoraQueryException => None }
}

object ZuoraSubscriptionService extends ZuoraSubscriptionService(ZuoraClient, Configuration.knownProducts, new CloudWatch {
  override val region: Region = Region.getRegion(Regions.EU_WEST_1)
  override val application: String = Configuration.appName
  override val service: String = "ZuoraSubscriptionService"
  override val stage: String = Configuration.stage
})

trait ZuoraClient {
  def queryForSubscription(subscriptionId:String): Future[Zuora.Subscription]
  def queryForRatePlan(subscriptionId: String): Future[Zuora.RatePlan]
  def queryForProductRatePlan(id: String): Future[Zuora.ProductRatePlan]
  def queryForAccount(id: String): Future[Zuora.Account]
  def queryForContact(id: String): Future[Zuora.Contact]
  def queryForProduct(id: String): Future[Zuora.Product]
}

object ZuoraClient extends ZuoraApi with ZuoraClient {
  import com.gu.membership.zuora.ZuoraApiConfig

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