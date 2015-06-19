package com.gu.subscriptions.cas.config

import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import spray.http.Uri


object Configuration {
  private val log = LoggerFactory.getLogger(getClass)
  private val appConfig = ConfigFactory.load()

  val EXPECTED_FIELDS = List(
    "proxy"
  )

  val stage = appConfig.getString("stage")

  val sentryDsn =
    if (appConfig.hasPath("sentry.dsn")) {
      log.info("Sentry DSN found, we will report errors to Sentry")
      Some(appConfig.getString("sentry.dsn"))
    } else {
      if (stage == "PROD") log.error("Setting 'sentry.dsn' is blank! The app will not be able to report errors to Sentry")
      None
    }

  val proxy = appConfig.getString("proxy")

  val (proxyScheme, proxyHost, proxyPort):(String,String,Int) = {
    val proxyUri = Uri(proxy)
    (proxyUri.scheme, proxyUri.authority.host.address, proxyUri.effectivePort)
  }

  val nullSettings = EXPECTED_FIELDS.filter(appConfig.getString(_) == null)
}
