package com.gu.subscriptions.cas.directives

import com.gu.subscriptions.cas.config.Configuration.nullSettings
import com.gu.subscriptions.cas.service.zuora.ZuoraSubscriptionService
import com.typesafe.scalalogging.LazyLogging
import spray.http.MediaTypes._
import spray.routing.{HttpService, Route}

trait HealthCheckDirective extends LazyLogging with ErrorRoute {this: HttpService =>
  val healthCheck: Route =
    (get & path("healthcheck") & respondWithMediaType(`application/json`)) {
      // Make sure that the inner route gets reevaluated each time, as the Zuora service availability changes
      dynamic {
        {
          if (nullSettings.nonEmpty) {
            throw new RuntimeException(s"Configuration keys not found: '$nullSettings'")
          }

          if (!ZuoraSubscriptionService.isReady) {
            logger.warn("Service not ready yet")
            serviceUnavailableError
          } else {
            complete(s"""{ "status": "ok" , "gitCommitId": "${app.BuildInfo.gitCommitId}" }""")
          }

        }
      }
    }
}
