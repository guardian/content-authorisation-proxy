package com.gu.subscriptions.cas.directives

import com.gu.memsub.Subscription.Name
import com.gu.subscriptions.cas.directives.ZuoraDirective.TriggersActivation
import com.gu.subscriptions.cas.model.SubscriptionRequest
import shapeless.{::, HNil}
import spray.routing.{Directive, Route}
import spray.routing.Directives._

object ZuoraDirective {
  type TriggersActivation = Boolean
  def zuoraDirective(subscriptionRequest: SubscriptionRequest): Directive[TriggersActivation :: Name :: HNil] = new ZuoraDirective(subscriptionRequest)
}

class ZuoraDirective(subscriptionRequest: SubscriptionRequest) extends Directive[TriggersActivation :: Name :: HNil] {
  override def happly(f: (TriggersActivation :: Name :: HNil) => Route): Route = {
    subscriptionRequest.subscriberId.filter(_.startsWith("A-S")) match {
      case Some(subId) =>
        parameter("noActivation") { _ =>
          f(false :: Name(subId) :: HNil)
        } ~ f(true :: Name(subId) :: HNil)
      case _ => reject
    }
  }
}
