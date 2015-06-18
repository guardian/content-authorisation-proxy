package com.gu.subscriptions.cas.directives

import com.gu.subscriptions.cas.config.Configuration.nullSettings
import spray.http.MediaTypes._
import spray.routing.{HttpService, Route}

trait HealthCheckDirective {this: HttpService =>
  val healthCheck: Route =
    get {
      path("healthcheck") {
        respondWithMediaType(`application/json`) {
          if (nullSettings.nonEmpty) {
            throw new RuntimeException(s"Configuration keys not found: '$nullSettings'")
          }
          complete( s"""{ "status": "ok" , "gitCommitId": "${app.BuildInfo.gitCommitId}" }""")
        }
      }
    }
}
