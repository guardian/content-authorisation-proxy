package com.gu.subscriptions.cas.service.zuora

import com.gu.membership.util.Timing
import com.gu.membership.zuora.soap.Zuora._
import com.gu.membership.zuora.soap._
import com.gu.subscriptions.cas.config.Configuration.knownProducts
import com.gu.subscriptions.cas.config.Zuora._
import com.gu.subscriptions.cas.model.Implicits.ContactOpts
import com.gu.subscriptions.cas.service.SubscriptionService
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object ZuoraSubscriptionService extends LazyLogging with SubscriptionService {
  override def updateActivationDate(subscription: Subscription): Unit = {
    val name = subscription.name
    if (subscription.activationDate.isEmpty) {
      ZuoraClient.updateSubscription(subscription.id, "ActivationDate__c" -> DateTime.now().toString) onComplete {
        case Success(_) => logger.debug(s"Updated activation date for subscription $name")
        case Failure(e) => logger.error(s"Error while trying to update activation date for subscription: $name", e)
      }
    } else {
      logger.debug(s"Activation date already present (${subscription.activationDate.get}}) in subscription $name")
    }
  }

  override def getValidSubscription(subscriptionName: String, password: String): Future[Option[Subscription]] = {
    /**
     * @return Some(Subscription) if the lookup was successful, None if the query an empty result set.
     */
    def checkSubscriptionValidity(subscription: Subscription): Future[Boolean] =
      for {
        productsMatch <- knownProductCheck(subscription)
        postcodesMatch <- postcodeCheck(subscription, password)
      } yield productsMatch && postcodesMatch

    def getSubscription: Future[Option[Subscription]] = ZuoraClient.queryForSubscriptionOpt(subscriptionName)

    for {
      subscription <- getSubscription
      isValid <- subscription.fold(Future {false}) { subscription => checkSubscriptionValidity(subscription) }
    } yield subscription.filter(_ => isValid)
  }

  override def isReady: Boolean = ZuoraClient.isReady

  private def knownProductCheck(subscription: Subscription): Future[Boolean] =
    for {
      ratePlan <- ZuoraClient.queryForRatePlan(subscription.id)
      productRatePlan <- ZuoraClient.queryForProductRatePlan(ratePlan.productRatePlanId)
      product <- ZuoraClient.queryForProduct(productRatePlan.productId)
    } yield knownProducts.contains(product.name)

  private def postcodeCheck(subscription: Subscription, postcode: String): Future[Boolean] =
    for {
      account <- ZuoraClient.queryForAccount(subscription.accountId)
      contact <- ZuoraClient.queryForContact(account.billToId)
    } yield {
      val postcodesMatch = contact.samePostcode(postcode)
      if (!postcodesMatch) {
        cloudWatch.put("Postcodes not matching", 1)
        logger.info(s"Postcodes not matching: ${contact.postalCode}, $postcode")
      }
      postcodesMatch
    }

  private object ZuoraClient {
    import ZuoraDeserializer._

    def queryForSubscription(subscriptionName: String): Future[Subscription] =
      queryForSubscriptionOpt(subscriptionName).map(_.getOrElse(
        throw new ZuoraQueryException(s"Subscription not found '$subscriptionName'")
      ))

    def queryForSubscriptionOpt(subscriptionName: String): Future[Option[Subscription]] =
      Timing.record(metrics, "queryForSubscription") {
        api.query[Subscription](s"Name='$subscriptionName'")
          .map(_.sortWith(_.version > _.version).headOption)
      }

    def queryForProduct(id: String): Future[Product] =
      api.queryOne[Product](s"Id='$id'")

    def queryForRatePlan(subscriptionId: String): Future[RatePlan] =
      api.queryOne[RatePlan](s"SubscriptionId='$subscriptionId'")

    def queryForProductRatePlan(id: String): Future[ProductRatePlan] =
      api.queryOne[ProductRatePlan](s"Id='$id'")

    def queryForAccount(id: String): Future[Account] =
      api.queryOne[Account](s"Id='$id'")

    def queryForContact(id: String): Future[Contact] =
      api.queryOne[Contact](s"Id='$id'")

    def updateSubscription(subscriptionId: String, fields: (String, String)*): Future[UpdateResult] = {
      api.authenticatedRequest[UpdateResult](Update(subscriptionId, "Subscription", fields))
    }

    def isReady: Boolean = api.isReady
  }
}

