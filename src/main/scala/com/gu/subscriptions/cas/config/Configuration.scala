package com.gu.subscriptions.cas.config

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._
import scala.util.Try


object Configuration {
  val appName = "content-authorisation-proxy"

  val appConfig = ConfigFactory.load()

  val EXPECTED_FIELDS = List(
    "proxy"
  )

  val stage = appConfig.getString("stage")

  val sentryDsn = Try(appConfig.getString("sentry.dsn"))

  val proxy = appConfig.getString("proxy")



  val nullSettings = EXPECTED_FIELDS.filter(appConfig.getString(_) == null)

  lazy val knownProducts = appConfig.getStringList("knownProducts").toList

  implicit val system = ActorSystem("subscriptions-cas")
}
