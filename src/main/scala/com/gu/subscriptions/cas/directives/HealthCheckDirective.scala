package com.gu.subscriptions.cas.directives

import com.gu.subscriptions.cas.Configuration.appConfig
import spray.http.MediaTypes._
import spray.routing.{HttpService, Route}

trait HealthCheckDirective {this: HttpService =>

  val checkConfig = List(
    "proxy"
  ).filter(appConfig.getString(_) == null)

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
