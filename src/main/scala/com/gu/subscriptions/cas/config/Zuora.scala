package com.gu.subscriptions.cas.config

import com.amazonaws.regions.{Region, Regions}
import com.gu.monitoring.{CloudWatch, ServiceMetrics}
import com.gu.subscriptions.cas.config.Configuration.{appName, stage => appStage, touchpointConfig}
import com.gu.zuora.soap.ClientWithFeatureSupplier
import com.gu.zuora.{ZuoraApiConfig, rest}

object Zuora {

  implicit val actorSystem = Configuration.system

  object Soap {
    val apiConfig = ZuoraApiConfig.soap(touchpointConfig, appStage)
    val metrics = new ServiceMetrics(appStage, appName, "zuora-soap-client")
    val client = new ClientWithFeatureSupplier(featureCodes = Set.empty,
                                               apiConfig = apiConfig,
                                               metrics = metrics)
  }

  object Rest {
    val apiConfig = ZuoraApiConfig.rest(touchpointConfig, appStage)
    val metrics = new ServiceMetrics(appStage, appName, "zuora-rest-client")
    val client = new rest.Client(apiConfig, metrics)
  }

  val cloudWatch = new CloudWatch {
    override val region: Region = Region.getRegion(Regions.EU_WEST_1)
    override val application: String = appName
    override val service: String = "ZuoraSubscriptionService"
    override val stage: String = Configuration.stage
  }
}
