package com.gu.subscriptions.cas.config

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import spray.http.Uri


object Configuration {
  private val appConfig = ConfigFactory.load()

  val EXPECTED_FIELDS = List(
    "proxy"
  )

  val stage = appConfig.getString("stage")

  val sentryDsn = Option(appConfig.getString("sentry.dsn"))

  val proxy = appConfig.getString("proxy")

  val (proxyScheme, proxyHost, proxyPort):(String,String,Int) = {
    val proxyUri = Uri(proxy)
    (proxyUri.scheme, proxyUri.authority.host.address, proxyUri.effectivePort)
  }

  val nullSettings = EXPECTED_FIELDS.filter(appConfig.getString(_) == null)
}
