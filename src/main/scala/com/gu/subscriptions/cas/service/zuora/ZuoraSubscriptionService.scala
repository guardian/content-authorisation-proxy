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
      ZuoraClient.updateSubscriptionActivationDate(subscription.id) onComplete {
        case Success(_) => logger.debug(s"Updated activation date for subscription $name")
        case Failure(e) => logger.error(s"Error while trying to update activation date for subscription: $name", e)
      }
    } else {
      logger.debug(s"Activation date already present (${subscription.activationDate.get}}) in subscription $name")
    }
  }

  override def getValidSubscription(subscriptionName: String, password: String): Future[Option[Subscription]] = {
    def checkSubscriptionValidity(subscription: Subscription): Future[Boolean] = {
      val knownProductCheck = for {
        ratePlan <- ZuoraClient.queryForRatePlan(subscription.id)
        productRatePlan <- ZuoraClient.queryForProductRatePlan(ratePlan.productRatePlanId)
        product <- ZuoraClient.queryForProduct(productRatePlan.productId)
      } yield knownProducts.contains(product.name)

      val postcodeCheck =
        for {
          account <- ZuoraClient.queryForAccount(subscription.accountId)
          contact <- ZuoraClient.queryForContact(account.billToId)
        } yield {
          val postcodesMatch = contact.samePostcode(password)
          if (!postcodesMatch) {
            cloudWatch.put("Postcodes not matching", 1)
            logger.info(s"Postcodes not matching: ${contact.postalCode}, $password")
          }
          postcodesMatch
        }

      for {
        productsMatch <- knownProductCheck
        postcodesMatch <- postcodeCheck
      } yield productsMatch && postcodesMatch
    }

    for {
      subscription <- ZuoraClient.queryForSubscriptionOpt(subscriptionName)
      isValid <- subscription.fold(Future {false})(checkSubscriptionValidity)
    } yield subscription.filter(_ => isValid)
  }

  override def isReady: Boolean = ZuoraClient.isReady

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

    def updateSubscriptionActivationDate(subscriptionId: String): Future[UpdateResult] = {
      val fields = Seq("ActivationDate__c" -> DateTime.now().toString)
      api.authenticatedRequest[UpdateResult](Update(subscriptionId, "Subscription",  fields))
    }

    def isReady: Boolean = api.isReady
  }
}

