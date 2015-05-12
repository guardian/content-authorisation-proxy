package com.gu.subscriptions.cas.bootstrap

import com.gu.subscriptions.cas.directives.{HealthCheckDirective, ProxyDirective}
import spray.routing._

class CASService extends HttpServiceActor
  with ProxyDirective
  with HealthCheckDirective {

  override def actorRefFactory = context

  override implicit val actorSystem = context.system

  override def receive = runRoute(healthCheck ~ proxyRoute)
}
