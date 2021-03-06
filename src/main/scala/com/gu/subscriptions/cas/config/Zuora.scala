package com.gu.subscriptions.cas.config

import com.gu.monitoring.ServiceMetrics
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
    val patientClient = new rest.SimpleClient[Future](apiConfig, configurableFutureRunner(60.seconds))
  }
}
