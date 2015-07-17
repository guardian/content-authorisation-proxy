package com.gu.subscriptions.cas.directives

import com.gu.subscriptions.cas.model.SubscriptionRequest
import shapeless.{HNil, ::}
import spray.routing.{Route, Directive1}
import spray.routing.directives.RouteDirectives.reject

object ZuoraDirective {
  def zuoraDirective(subscriptionRequest: SubscriptionRequest): Directive1[String] = new ZuoraDirective(subscriptionRequest)
}

class ZuoraDirective(subscriptionRequest: SubscriptionRequest) extends Directive1[String] {
  override def happly(f: (String :: HNil) => Route): Route = {
    subscriptionRequest.subscriberId.filter(_.startsWith("A-S")) match {
      case Some(subId) => f(subId :: HNil)
      case _ => reject
    }
  }
}
