package com.gu.subscriptions.cas.bootstrap

import akka.actor.Props
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.gu.config.{DiscountRatePlan, DiscountRatePlanIds}
import com.gu.memsub.Subscription.{ProductRatePlanChargeId, ProductRatePlanId}
import com.gu.memsub.services.{SubscriptionService => CommonSubscriptionService, PromoService, CatalogService}
import com.gu.monitoring.ServiceMetrics
import com.gu.stripe.{StripeApiConfig, StripeService}
import com.gu.subscriptions.Discounter
import com.gu.subscriptions.cas.config.Configuration.{system, _}
import com.gu.subscriptions.cas.config.Zuora.{Rest, Soap}
import com.gu.subscriptions.cas.service.SubscriptionService
import com.gu.zuora.ZuoraService
import spray.can.Http

import scala.concurrent.duration._

object Bootstrap extends App {

  SentryLogging.init()

  implicit val ec = system.dispatcher

  //This application does not use Stripe
  val stripeService = {
    val stripeApiConfig = StripeApiConfig.from(touchpointConfig, stage)
    val stripeMetrics = new ServiceMetrics(stage, "CAS proxy", "Stripe")
    new StripeService(stripeApiConfig, stripeMetrics)
  }

  val zuoraService = new ZuoraService(Soap.client, Rest.client, digipackPlans)
  val catalogService = CatalogService(Rest.client, membershipPlans, digipackPlans, stage)

  val commonSubscriptionService = new CommonSubscriptionService(zuoraService, stripeService, catalogService.digipackCatalog)
  val subscriptionService = new SubscriptionService(zuoraService, commonSubscriptionService, catalogService)
  val service = system.actorOf(Props(classOf[CASService], subscriptionService))

  implicit val timeout = Timeout(5.seconds)

  IO(Http) ? Http.Bind(service, interface = "0.0.0.0", port = 9300)
}
