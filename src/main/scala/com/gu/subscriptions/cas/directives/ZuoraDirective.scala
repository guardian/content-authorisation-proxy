package com.gu.subscriptions.cas.directives

import com.gu.memsub.Subscription.Name
import com.gu.subscriptions.cas.directives.ZuoraDirective.{TriggersActivation, noSubscriberIdPassedToSubs}
import com.gu.subscriptions.cas.model.SubscriptionRequest
import com.gu.subscriptions.cas.monitoring.Histogram
import shapeless.{::, HNil}
import spray.routing.{Directive, Route}
import spray.routing.Directives._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object ZuoraDirective {
  type TriggersActivation = Boolean
  def zuoraDirective(subscriptionRequest: SubscriptionRequest): Directive[TriggersActivation :: Name :: HNil] = new ZuoraDirective(subscriptionRequest)
  val noSubscriberIdPassedToSubs = new Histogram("noSubscriberIdPassedToSubs", 1, DAYS)
}

class ZuoraDirective(subscriptionRequest: SubscriptionRequest) extends Directive[TriggersActivation :: Name :: HNil] {
  override def happly(f: (TriggersActivation :: Name :: HNil) => Route): Route = {
    subscriptionRequest.subscriberId match {
      case Some(subId) =>
        parameter("noActivation") { _ =>
          f(false :: Name(subId) :: HNil)
        } ~ f(true :: Name(subId) :: HNil)
      case _ =>
        noSubscriberIdPassedToSubs.count("NoSubscriberIdPassedToSubs") // clients should not be doing this
        reject
    }
  }
}
