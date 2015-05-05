package com.gu.subscriptions.cas.bootstrap

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http

import scala.concurrent.duration._

object Bootstrap extends App {

  implicit val system = ActorSystem("subscriptions-cas")

  val service = system.actorOf(Props[CASService])

  implicit val timeout = Timeout(5.seconds)

  IO(Http) ? Http.Bind(service, interface = "0.0.0.0", port = 9300)
}
