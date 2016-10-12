package com.gu.subscriptions.cas.service

import com.github.nscala_time.time.Imports._
import com.gu.memsub.Digipack
import com.gu.memsub.Subscription.Name
import com.gu.memsub.subsv2.Subscription
import com.gu.memsub.subsv2.SubscriptionPlan.Paid
import com.gu.memsub.subsv2.reads.ChargeListReads._
import com.gu.memsub.subsv2.services.{ SubscriptionService => CommonSubscriptionService }
import com.gu.subscriptions.cas.config.Zuora._
import com.gu.subscriptions.cas.model.ContactOps.WithMatchingPassword
import com.gu.zuora.api.ZuoraService
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future

class SubscriptionService(zuoraService: ZuoraService,
                          commonSubscriptionService: CommonSubscriptionService[Future]) extends api.SubscriptionService with LazyLogging {
  def getValidSubscription(subscriptionName: Name, password: String): Future[Option[Subscription[Paid]]] = {
    def checkSubscriptionValidity(subscription: Subscription[Paid]): Future[Boolean] = {
      val postcodeCheck =
        if (subscription.plan.charges.benefits.list.contains(Digipack)) {
          for {
            account <- zuoraService.getAccount(subscription.accountId)
            contact <- zuoraService.getContact(account.billToId)
          } yield {
            val isValid = contact.matchesPassword(password)
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
