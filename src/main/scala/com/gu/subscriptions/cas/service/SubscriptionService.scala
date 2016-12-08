package com.gu.subscriptions.cas.service

import com.github.nscala_time.time.Imports._
import com.gu.memsub.Digipack
import com.gu.memsub.Subscription.Name
import com.gu.memsub.subsv2.Subscription
import com.gu.memsub.subsv2.SubscriptionPlan.Paid
import com.gu.memsub.subsv2.reads.ChargeListReads._
import com.gu.memsub.subsv2.services.{SubscriptionService => CommonSubscriptionService}
import com.gu.salesforce.SimpleContactRepository
import com.gu.subscriptions.cas.config.Zuora._
import com.gu.subscriptions.cas.model.ContactOps._
import com.gu.zuora.api.ZuoraService
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future

class SubscriptionService(zuoraService: ZuoraService,
                          commonSubscriptionService: CommonSubscriptionService[Future],
                          sfContactRepo: SimpleContactRepository) extends api.SubscriptionService with LazyLogging {

  def getValidSubscription(subscriptionName: Name, password: String): Future[Option[Subscription[Paid]]] = {
    def checkSubscriptionValidity(subscription: Subscription[Paid]): Future[Boolean] = {
      val postcodeCheck =
        if (subscription.plan.charges.benefits.list.contains(Digipack)) {
          for {
            zuoraAccount <- zuoraService.getAccount(subscription.accountId)
            billingContact <- zuoraService.getContact(zuoraAccount.billToId)
            sfContactId <- zuoraAccount.sfContactId.fold[Future[String]](Future.failed(new IllegalStateException(s"Zuora record for ${subscription.accountId} has no sfContactId")))(Future.successful)
            deliveryContact <- sfContactRepo.getByContactId(sfContactId)
          } yield {
            val isValid = billingContact.matchesPassword(password) || deliveryContact.exists(_.matchesPassword(password))
            if (!isValid) {
              cloudWatch.put("Postcode or last name does not match", 1)
            }
            isValid
          }
        } else {
          Future.successful(false)
        }

      for {
        postcodesMatch <- postcodeCheck
      } yield postcodesMatch
    }

    for {
      subscription <- commonSubscriptionService.get[Paid](subscriptionName)
      isValid <- subscription.fold(Future.successful(false))(checkSubscriptionValidity)
    } yield subscription.filter(_ => isValid)
  }

  def isReady: Boolean = zuoraService.lastPingTimeWithin(2.minutes)

  def updateActivationDate(subscription: Subscription[Paid]): Unit = {
    val name = subscription.name
    subscription.casActivationDate.fold {
      zuoraService.updateActivationDate(subscription.id)
    } { date =>
      logger.debug(s"Activation date already present ($date) in subscription $name")
      Future.successful(())
    }
  }
}
