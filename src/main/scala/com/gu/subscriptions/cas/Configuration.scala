package com.gu.subscriptions.cas

import com.typesafe.config.{Config, ConfigFactory}

object Configuration {
  val appConfig: Config = ConfigFactory.load()

  val proxy = appConfig.getString("proxy")

  val (proxyScheme, proxyHost, proxyPort):(String,String,Int) = {
    val urlParser = "(\\w+)://([^:/]+):?(\\d+)?/?".r
    val urlParser(scheme, host, optionalPort) = proxy
    val port: Int = Option(optionalPort).filterNot(_ == null).map(_.toInt).getOrElse(if(scheme.toLowerCase == "https") 443 else 80)
    (scheme, host, port)
  }

}
