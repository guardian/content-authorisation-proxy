package com.gu.subscriptions.cas.config

import akka.actor.ActorSystem
import com.gu.cas.PrefixedTokens
import com.gu.config.{DigitalPackRatePlanIds, MembershipRatePlanIds, SubsV2ProductIds, SubscriptionsProductIds}
import com.gu.memsub.Digipack
import com.typesafe.config.ConfigFactory

import scala.util.Try

object Configuration {
  val appName = "content-authorisation-proxy"

  val appConfig = ConfigFactory.load()

  val EXPECTED_FIELDS = List("proxy")

  val stage = appConfig.getString("stage")

  val dynamoAuthTableNAme = appConfig.getString("dynamo.auth.table")

  val sentryDsn = Try(appConfig.getString("sentry.dsn"))

  val proxy = appConfig.getString("proxy")

  val nullSettings = EXPECTED_FIELDS.filter(appConfig.getString(_) == null)

  implicit val system = ActorSystem("subscriptions-cas")

  val touchpointConfig = appConfig.getConfig("touchpoint.backend.environments").getConfig(stage)

  val digipackPlans = DigitalPackRatePlanIds.fromConfig(touchpointConfig.getConfig("zuora.ratePlanIds.digitalpack"))
  val membershipPlans = MembershipRatePlanIds.fromConfig(touchpointConfig.getConfig("zuora.ratePlanIds.membership"))
  val subsIds = SubscriptionsProductIds(touchpointConfig.getConfig("zuora.productIds.subscriptions"))

  val generalErrorCode = appConfig.getInt("general.error.code")
  val unhandledExceptionErrorCode = appConfig.getInt("unhandled.exception.error.code")
  val networkErrorCode = appConfig.getInt("network.error.code")
  val malformedRequestErrorCode = appConfig.getInt("malformed.request.error.code")
  val mandatoryDataMissingErrorCode = appConfig.getInt("mandatory.data.missing.error.code")
  val noFreePeriodForDeviceErrorCode = appConfig.getInt("no.free.period.for.device.error.code")
  val credentialsOverUseErrorCode = appConfig.getInt("credentials.overuse.error.code")
  val invalidSubscriptionTypeErrorCode = appConfig.getInt("invalid.subscription.type.error.code")
  val unknownSubscriberErrorCode = appConfig.getInt("unknown.subscriber.error.code")
  val subscriptionDisabledErrorCode = appConfig.getInt("subscription.disabled.error.code")
  val authFreeperiodAlreadyset = appConfig.getInt("auth.freeperiod.alreadyset")
  val authFreeperiodTofarinthefuture = appConfig.getInt("auth.freeperiod.tofarinthefuture")

  implicit val productFamily = Digipack

  def productIds(env: String): com.gu.memsub.subsv2.reads.ChargeListReads.ProductIds =
    SubsV2ProductIds(touchpointConfig.getConfig("zuora.productIds"))
  object EmergencyTokens {
    private val conf = appConfig.getConfig("cas.emergency.subscriber.auth")
    val prefix = conf.getString("prefix")
    val secret = conf.getString("secret")
  }


}
