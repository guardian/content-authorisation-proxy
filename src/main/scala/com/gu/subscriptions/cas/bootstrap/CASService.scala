package com.gu.subscriptions.cas.bootstrap

import com.gu.subscriptions.cas.directives.{CheckSentryErrors, HealthCheckDirective, ProxyDirective}
import spray.routing._

class CASService extends HttpServiceActor
  with ProxyDirective
  with HealthCheckDirective
  with CheckSentryErrors {

  override def actorRefFactory = context

  override implicit val actorSystem = context.system

  override def receive = runRoute(healthCheck ~ proxyRoute ~ testSentryErrors)
}
