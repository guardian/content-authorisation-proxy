package com.gu.subscriptions.cas.config

import com.amazonaws.regions.{Region, Regions}
import com.gu.monitoring.{CloudWatch, ServiceMetrics}
import com.gu.okhttp.RequestRunners._
import com.gu.subscriptions.cas.config.Configuration.{appName, touchpointConfig, stage => appStage}
import com.gu.zuora.{ZuoraApiConfig, rest, soap}

import scala.concurrent.Future
import scala.concurrent.duration._
import scalaz.std.scalaFuture._

object Zuora {

  implicit val actorSystem = Configuration.system
  implicit val ec = actorSystem.dispatcher

  object Soap {
    val apiConfig = ZuoraApiConfig.soap(touchpointConfig, appStage)
    val metrics = new ServiceMetrics(Configuration.stage, Configuration.appName, "zuora-soap-client")
    val client = new soap.ClientWithFeatureSupplier(Set.empty, apiConfig, loggingRunner(metrics), configurableLoggingRunner(20.seconds, metrics))
    val simpleClient = new rest.SimpleClient[Future](Rest.apiConfig, futureRunner)
  }

  object Rest {
    val apiConfig = ZuoraApiConfig.rest(touchpointConfig, appStage)
    val metrics = new ServiceMetrics(appStage, appName, "zuora-rest-client")
    val simpleClient = new rest.SimpleClient[Future](apiConfig, futureRunner)
  }

  val cloudWatch = new CloudWatch {
    override val region: Region = Region.getRegion(Regions.EU_WEST_1)
    override val application: String = appName
    override val service: String = "ZuoraSubscriptionService"
    override val stage: String = Configuration.stage
  }
}
