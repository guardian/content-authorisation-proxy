package com.gu.subscriptions.cas.service

import com.github.nscala_time.time.Imports._
import com.gu.memsub.{Digipack, PaidPS, PaymentStatus, Subscription}
import com.gu.memsub.Subscription.Name
import com.gu.memsub.services.CatalogService
import com.gu.memsub.services.api.{SubscriptionService => CommonSubscriptionService}
import com.gu.subscriptions.{DigipackCatalog, PaperCatalog, ProductList, ProductPlan}
import com.gu.subscriptions.cas.config.Configuration.productFamily
import com.gu.subscriptions.cas.config.Zuora._
import com.gu.subscriptions.cas.model.ContactOps.WithMatchingPassword
import com.gu.zuora.api.ZuoraService
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubscriptionService(zuoraService: ZuoraService, commonSubscriptionService: CommonSubscriptionService[PaperCatalog], catalogService: CatalogService) extends api.SubscriptionService with LazyLogging  {
  def getValidSubscription(subscriptionName: Name, password: String): Future[Option[Subscription]] = {
    def checkSubscriptionValidity(subscription: Subscription with PaymentStatus[ProductPlan[ProductList]]): Future[Boolean] = {
      if(subscription.plan.products.seq.contains(Digipack)) {
        for {
          account <- zuoraService.getAccount(subscription.accountId)
          contact <- zuoraService.getContact(account.billToId)
        } yield {
          val isValid = contact.matchesPassword(password)
          if (!isValid) cloudWatch.put("Postcode or last name does not match", 1)
          isValid
        }
      } else {
        Future.successful(false)
      }
    }

    for {
      subscription <- commonSubscriptionService.get(subscriptionName)
      isValid <- subscription.fold(Future.successful(false))(checkSubscriptionValidity)
    } yield subscription.filter(_ => isValid)
  }

  def isReady: Boolean = zuoraService.lastPingTimeWithin(2.minutes)

  def updateActivationDate(subscription: Subscription): Unit =
    commonSubscriptionService.updateActivationDate(subscription)
}

