package com.gu.subscriptions.cas.bootstrap

import akka.actor.Props
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http

import scala.concurrent.duration._
import com.gu.subscriptions.cas.config.Configuration.system

object Bootstrap extends App {

  SentryLogging.init()

  val service = system.actorOf(Props[CASService])

  implicit val timeout = Timeout(5.seconds)

  IO(Http) ? Http.Bind(service, interface = "0.0.0.0", port = 9300)
}
