package com.gu.subscriptions.cas.bootstrap

import spray.routing._

class CASService extends HttpServiceActor {

  override def actorRefFactory = context

  override def receive = runRoute(complete("OK"))
}
