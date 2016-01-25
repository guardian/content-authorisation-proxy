package com.gu.subscriptions.cas.service

import com.github.nscala_time.time.Imports._
import com.gu.subscriptions.cas.config.Configuration._
import com.gu.subscriptions.cas.config.Zuora._
import com.gu.subscriptions.cas.model.Implicits.ContactOpts
import com.gu.zuora.api.ZuoraService
import com.gu.zuora.soap.models.Queries.Subscription
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubscriptionService(zuoraService: ZuoraService) extends api.SubscriptionService with LazyLogging  {
  def getValidSubscription(subscriptionName: String, password: String): Future[Option[Subscription]] = {
    def checkSubscriptionValidity(subscription: Subscription): Future[Boolean] = {
      val knownProductCheck = for {
        ratePlan <- zuoraService.getRatePlan(subscription)
        productRatePlan <- zuoraService.getProductRatePlan(ratePlan)
        product <- zuoraService.getProduct(productRatePlan)
      } yield knownProducts.contains(product.name)

      val postcodeCheck =
        for {
          account <- zuoraService.getAccount(subscription)
          contact <- zuoraService.getContact(account.billToId)
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
      subscription <- zuoraService.getLatestSubscriptionByName(subscriptionName)
      isValid <- subscription.fold(Future {false})(checkSubscriptionValidity)
    } yield subscription.filter(_ => isValid)
  }

  def isReady: Boolean = zuoraService.lastPingTimeWithin(2.minutes)

  override def updateActivationDate(subscription: Subscription): Unit =
    zuoraService.updateActivationDate(subscription)
}

