package com.gu.subscriptions.cas.bootstrap

import com.gu.subscriptions.cas.directives.{CheckSentryErrors, ErrorRoute, HealthCheckDirective, ProxyDirective}
import com.gu.subscriptions.cas.service.SubscriptionService
import com.typesafe.scalalogging.LazyLogging
import spray.routing._
import spray.util.LoggingContext
import spray.http.MediaTypes._

class CASService(val subscriptionService: SubscriptionService) extends HttpServiceActor
  with ProxyDirective
  with HealthCheckDirective
  with CheckSentryErrors
  with ErrorRoute
  with LazyLogging {

  override def actorRefFactory = context

  override implicit val actorSystem = context.system

  implicit def exceptionHandler(implicit log: LoggingContext) = ExceptionHandler {
      case e: Throwable =>
        logger.error("Generic error", e)
        serverError
  }

  override def receive = runRoute(
    respondWithMediaType(`application/json`) {
      healthCheck ~ authRoute ~ subsRoute ~ notFound
    }
  )
}
