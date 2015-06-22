package com.gu.subscriptions.cas.monitoring

trait StatusMetrics extends CloudWatch {
  def putResponseCode(status: Int, responseMethod: String) {
    val statusClass = status / 100
    put(s"${statusClass}XX-response-code", 1, responseMethod)
  }
}

trait RequestMetrics extends CloudWatch {
  def putRequest {
    put("request-count", 1)
  }
}