package com.gu.subscriptions.cas.config

import com.amazonaws.regions.{Region, Regions}
import com.gu.membership.zuora.ZuoraApiConfig
import com.gu.membership.zuora.soap.ZuoraApi
import com.gu.monitoring.{CloudWatch, ZuoraMetrics}
import com.gu.subscriptions.cas.config.Configuration.{appConfig, appName, system, stage => appStage}

object Zuora {
  private val zuoraConfig = appConfig.getConfig("touchpoint.backend.environments").getConfig(appStage)
  private val apiConfig: ZuoraApiConfig = ZuoraApiConfig.from(zuoraConfig, appStage)

  val metrics = new ZuoraMetrics(appStage, appName)
  val api = new ZuoraApi(apiConfig, metrics, system)
  val cloudWatch = new CloudWatch {
    override val region: Region = Region.getRegion(Regions.EU_WEST_1)
    override val application: String = appName
    override val service: String = "ZuoraSubscriptionService"
    override val stage: String = Configuration.stage
  }
}
