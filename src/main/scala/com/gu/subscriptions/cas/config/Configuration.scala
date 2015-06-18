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

  val sentryDsn = (stage, appConfig.hasPath("sentry.dsn")) match {
    case ("PROD", true) =>
      Some(appConfig.getString("sentry.dsn"))
    case ("PROD", false) =>
      log.warn("Setting 'sentry.dsn' is blank! The app will not be able to report errors to Sentry")
      None
    case _ =>
      None
  }

  val proxy = appConfig.getString("proxy")

  val (proxyScheme, proxyHost, proxyPort):(String,String,Int) = {
    val proxyUri = Uri(proxy)
    (proxyUri.scheme, proxyUri.authority.host.address, proxyUri.effectivePort)
  }

  val nullSettings = EXPECTED_FIELDS.filter(appConfig.getString(_) == null)
}
