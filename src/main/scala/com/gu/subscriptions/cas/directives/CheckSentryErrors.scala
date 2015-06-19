package com.gu.subscriptions.cas.directives

import spray.routing.{HttpService, Route}

trait CheckSentryErrors { this: HttpService =>
  val testSentryErrors: Route = get {
    path("error") {
      lazy val n = 1 / 0
      complete(n.toString)
    }
  }
}
