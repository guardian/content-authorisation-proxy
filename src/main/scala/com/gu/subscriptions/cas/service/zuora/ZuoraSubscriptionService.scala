package com.gu.subscriptions.cas.service.zuora

import com.amazonaws.regions.{Region, Regions}
import com.gu.membership.util.Timing
import com.gu.membership.zuora.soap.Zuora._
import com.gu.membership.zuora.soap._
import com.gu.monitoring.{CloudWatch, ZuoraMetrics}
import com.gu.subscriptions.cas.config.Configuration
import com.gu.subscriptions.cas.service.SubscriptionService
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

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
  override def checkSubscriptionValidity(subscription: Subscription, postcode: String): Future[Boolean] =
    for {
      productsMatch <- knownProductCheck(subscription)
      postcodesMatch <- postcodeCheck(subscription, postcode)
    } yield productsMatch && postcodesMatch

  override def updateActivationDate(subscription: Subscription): Unit = {
    val name = subscription.name
    if (subscription.activationDate.isEmpty) {
      zuoraClient.updateSubscription(subscription.id, "ActivationDate__c" -> DateTime.now().toString) onComplete {
        case Success(_) => logger.debug(s"Updated activation date for subscription $name")
        case Failure(e) => logger.error(s"Error while trying to update activation date for subscription: $name", e)
      }
    } else {
      logger.debug(s"Activation date already present (${subscription.activationDate.get}}) in subscription $name")
    }
  }

  override def getSubscription(name: String): Future[Option[Subscription]] = zuoraClient.queryForSubscriptionOpt(name)
}

object ZuoraSubscriptionService extends ZuoraSubscriptionService(ZuoraClient, Configuration.knownProducts, new CloudWatch {
  override val region: Region = Region.getRegion(Regions.EU_WEST_1)
  override val application: String = Configuration.appName
  override val service: String = "ZuoraSubscriptionService"
  override val stage: String = Configuration.stage
})

trait ZuoraClient {
  def queryForSubscription(subscriptionName: String): Future[Subscription]
  def queryForSubscriptionOpt(subscriptionName: String): Future[Option[Subscription]]
  def queryForRatePlan(subscriptionId: String): Future[RatePlan]
  def queryForProductRatePlan(id: String): Future[ProductRatePlan]
  def queryForAccount(id: String): Future[Account]
  def queryForContact(id: String): Future[Contact]
  def queryForProduct(id: String): Future[Product]
  def updateSubscription(subscriptionId: String, fields: (String, String)*): Future[UpdateResult]
}

object ZuoraClient extends ZuoraClient {
  import ZuoraDeserializer._
  import com.gu.membership.zuora.ZuoraApiConfig

  private val application: String = Configuration.appName
  private val stage: String = Configuration.stage
  val apiConfig: ZuoraApiConfig = ZuoraApiConfig.from(Configuration.zuoraConfig, Configuration.stage)
  val metrics = new ZuoraMetrics(stage, application)
  val api = new ZuoraApi(apiConfig, metrics, Configuration.system)

  def queryForSubscription(subscriptionName: String): Future[Zuora.Subscription] =
    queryForSubscriptionOpt(subscriptionName).map(_.getOrElse(
      throw new ZuoraQueryException(s"Subscription not found '$subscriptionName'")
    ))

  def queryForSubscriptionOpt(subscriptionName: String): Future[Option[Zuora.Subscription]] =
    Timing.record(metrics, "queryForSubscription") {
      api.query[Zuora.Subscription](s"Name='$subscriptionName'")
        .map(_.sortWith(_.version > _.version).headOption)
    }

  def queryForProduct(id: String): Future[Zuora.Product] =
    api.queryOne[Zuora.Product](s"Id='$id'")

  def queryForRatePlan(subscriptionId: String): Future[Zuora.RatePlan] =
    api.queryOne[Zuora.RatePlan](s"SubscriptionId='$subscriptionId'")

  def queryForProductRatePlan(id: String): Future[Zuora.ProductRatePlan] =
    api.queryOne[Zuora.ProductRatePlan](s"Id='$id'")

  def queryForAccount(id: String): Future[Zuora.Account] =
    api.queryOne[Zuora.Account](s"Id='$id'")

  def queryForContact(id: String): Future[Zuora.Contact] =
    api.queryOne[Zuora.Contact](s"Id='$id'")

  def updateSubscription(subscriptionId: String, fields: (String, String)*): Future[UpdateResult] = {
    api.authenticatedRequest[UpdateResult](Update(subscriptionId, "Subscription", fields))
  }
}