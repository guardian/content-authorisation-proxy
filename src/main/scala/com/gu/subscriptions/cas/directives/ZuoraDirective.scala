package com.gu.subscriptions.cas.directives

import com.gu.subscriptions.cas.directives.ZuoraDirective.{SubscriptionNumber, TriggersActivation}
import com.gu.subscriptions.cas.model.SubscriptionRequest
import shapeless.{::, HNil}
import spray.routing.{Directive, Route}
import spray.routing.Directives._

object ZuoraDirective {
  type TriggersActivation = Boolean
  type SubscriptionNumber = String
  def zuoraDirective(subscriptionRequest: SubscriptionRequest): Directive[TriggersActivation :: SubscriptionNumber :: HNil] = new ZuoraDirective(subscriptionRequest)
}

class ZuoraDirective(subscriptionRequest: SubscriptionRequest) extends Directive[TriggersActivation :: SubscriptionNumber :: HNil] {
  override def happly(f: (TriggersActivation :: SubscriptionNumber :: HNil) => Route): Route = {
    subscriptionRequest.subscriberId.filter(_.startsWith("A-S")) match {
      case Some(subId) =>
        parameter("noActivation") { _ =>
          f(false :: subId :: HNil)
        } ~ f(true :: subId :: HNil)
      case _ => reject
    }
  }
}
