package com.gu.subscriptions.cas.bootstrap
import akka.actor.Props
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.gu.memsub.subsv2.services.SubscriptionService._
import com.gu.memsub.subsv2.services.{CatalogService, SubscriptionService => CommonSubscriptionService}
import com.gu.monitoring.ServiceMetrics
import com.gu.subscriptions.cas.config.Configuration.{appName, productIds, stage, system}
import com.gu.subscriptions.cas.config.Zuora.{Rest, Soap}
import com.gu.subscriptions.cas.service.{DataStore, SubscriptionService}
import com.gu.zuora.ZuoraService
import org.joda.time.LocalDate
import spray.can.Http

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scalaz.std.scalaFuture._

object Bootstrap extends App {

  SentryLogging.init()

  implicit val ec = system.dispatcher

  private val soapServiceMetrics = new ServiceMetrics(stage, appName, "zuora-soap-client")
  private val newProductIds = productIds(stage)

  val zuoraService = new ZuoraService(Soap.client)
  val catalogService = new CatalogService[Future](newProductIds, Rest.patientClient, Await.result(_, 60.seconds), stage)

  private val map = this.catalogService.catalog.map(_.fold[CatalogMap](error => {println(s"error: ${error.list.mkString}"); Map()}, _.map))
  val commonSubscriptionService = new CommonSubscriptionService[Future](newProductIds, map, Rest.simpleClient, zuoraService.getAccountIds)
  val subscriptionService = new SubscriptionService(zuoraService, commonSubscriptionService)
  val dataStore = new DataStore
  val service = system.actorOf(Props(classOf[CASService], subscriptionService, dataStore))

  implicit val timeout = Timeout(5.seconds)

  IO(Http) ? Http.Bind(service, interface = "0.0.0.0", port = 9300)
}
