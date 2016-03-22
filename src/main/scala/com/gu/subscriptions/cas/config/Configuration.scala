package com.gu.subscriptions.cas.config

import akka.actor.ActorSystem
import com.gu.config.{MembershipRatePlanIds, DigitalPackRatePlanIds}
import com.gu.memsub.Digipack
import com.typesafe.config.ConfigFactory

import scala.util.Try

object Configuration {
  val appName = "content-authorisation-proxy"

  val appConfig = ConfigFactory.load()

  val EXPECTED_FIELDS = List("proxy")

  val stage = appConfig.getString("stage")

  val sentryDsn = Try(appConfig.getString("sentry.dsn"))

  val proxy = appConfig.getString("proxy")

  val nullSettings = EXPECTED_FIELDS.filter(appConfig.getString(_) == null)

  implicit val system = ActorSystem("subscriptions-cas")

  val touchpointConfig = appConfig.getConfig("touchpoint.backend.environments").getConfig(stage)

  val digipackPlans = DigitalPackRatePlanIds.fromConfig(touchpointConfig.getConfig("zuora.ratePlanIds.digitalpack"))
  val membershipPlans = MembershipRatePlanIds.fromConfig(touchpointConfig.getConfig("zuora.ratePlanIds.membership"))
  implicit val productFamily = Digipack
}
