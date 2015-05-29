package com.gu.subscriptions.cas.monitoring

import com.amazonaws.regions.{Region, Regions}
import com.gu.subscriptions.cas.config.Configuration

object Metrics extends CloudWatch {
  val region = Region.getRegion(Regions.EU_WEST_1)
  val stage = Configuration.stage
  val application = "content-auth-proxy"
}