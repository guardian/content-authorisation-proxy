package com.gu.subscriptions.cas.bootstrap

import com.gu.subscriptions.cas.directives.{CheckSentryErrors, HealthCheckDirective, ProxyDirective}
import com.typesafe.scalalogging.LazyLogging
import spray.json.{JsString, JsObject}
import spray.routing._
import spray.util.LoggingContext

class CASService extends HttpServiceActor
  with ProxyDirective
  with HealthCheckDirective
  with CheckSentryErrors
  with ServiceInitialiser
  with LazyLogging {

  override def actorRefFactory = context

  override implicit val actorSystem = context.system

  implicit def exceptionHandler(implicit log: LoggingContext) = ExceptionHandler {
      case e: Exception => ctx =>
        logger.error("Generic error", e)
        ctx.complete(JsObject("error" -> JsObject("message" -> JsString(e.getMessage))).toString())
  }

  override def receive = runRoute(healthCheck ~ proxyRoute ~ testSentryErrors)

  init()
}
