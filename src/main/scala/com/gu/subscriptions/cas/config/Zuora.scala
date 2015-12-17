package com.gu.subscriptions.cas.config

import com.amazonaws.regions.{Region, Regions}
import com.gu.config.DigitalPack
import com.gu.zuora.soap.ClientWithFeatureSupplier
import com.gu.zuora.{rest, soap, ZuoraApiConfig}
import com.gu.monitoring.{CloudWatch, ServiceMetrics}
import com.gu.subscriptions.cas.config.Configuration.{appConfig, appName, stage => appStage}

object Zuora {
  private val zuoraConfig = appConfig.getConfig("touchpoint.backend.environments").getConfig(appStage)

  object Soap {
    val apiConfig = ZuoraApiConfig.soap(zuoraConfig, appStage)
    val metrics = new ServiceMetrics(appStage, appName, "zuora-soap-client")
    val client = new ClientWithFeatureSupplier(featureCodes = Set.empty,
                                               apiConfig = apiConfig,
                                               metrics = metrics,
                                               actorSystem = Configuration.system)
  }

  object Rest {
    val apiConfig = ZuoraApiConfig.rest(zuoraConfig, appStage)
    val metrics = new ServiceMetrics(appStage, appName, "zuora-rest-client")
    val client = new rest.Client(apiConfig, metrics)
  }

  val productFamily = DigitalPack.fromConfig(zuoraConfig.getConfig("zuora.ratePlanIds"))

  val cloudWatch = new CloudWatch {
    override val region: Region = Region.getRegion(Regions.EU_WEST_1)
    override val application: String = appName
    override val service: String = "ZuoraSubscriptionService"
    override val stage: String = Configuration.stage
  }
}
