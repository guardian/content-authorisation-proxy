package com.gu.subscriptions.cas.config

import com.typesafe.config.ConfigFactory
import spray.http.Uri

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

  val (proxyScheme, proxyHost, proxyPort):(String,String,Int) = {
    val proxyUri = Uri(proxy)
    (proxyUri.scheme, proxyUri.authority.host.address, proxyUri.effectivePort)
  }

  val nullSettings = EXPECTED_FIELDS.filter(appConfig.getString(_) == null)

  val zuoraConfig = appConfig.getConfig("touchpoint.backend.environments").getConfig(stage)
}
