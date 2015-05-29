package com.gu.subscriptions.cas.config

import com.typesafe.config.{Config, ConfigFactory}
import spray.http.Uri

object Configuration {
  val appConfig: Config = ConfigFactory.load()

  val proxy = appConfig.getString("proxy")

  val (proxyScheme, proxyHost, proxyPort):(String,String,Int) = {
    val proxyUri = Uri(proxy)
    (proxyUri.scheme, proxyUri.authority.host.address, proxyUri.effectivePort)
  }

  val stage: String = appConfig.getString("stage")

}
