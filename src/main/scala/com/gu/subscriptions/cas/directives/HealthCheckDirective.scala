package com.gu.subscriptions.cas.directives

import com.typesafe.config.ConfigFactory
import spray.http.MediaTypes._
import spray.routing.{HttpService, Route}

trait HealthCheckDirective {this: HttpService =>

  val conf = ConfigFactory.load()

  val checkConfig = List(
    "proxy.url"
  ).filter(conf.getString(_) == null)

  val healthCheck: Route =
    get {
      path("healthcheck") {
        respondWithMediaType(`application/json`) {
          if (checkConfig.length > 0) {
            throw new RuntimeException(s"Configuration keys not found: '$checkConfig'")
          }
          complete( """{ "status": "ok" }""")
        }
      }
    }
}
