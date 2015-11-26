package com.gu.subscriptions.cas.model

import com.typesafe.scalalogging.LazyLogging
import spray.http.HttpResponse

object ResponseComparer extends LazyLogging {
  def compare(response: HttpResponse, other: HttpResponse): Unit = {
    def isSame = {
      val hasSameStatus = response.status == other.status
      val hasSameBody = response.entity.asString == other.entity.asString
      hasSameBody && hasSameStatus
    }
    if (!isSame)
      logger.error("Legacy apps returned different Responses")
    else
      logger.info("Legacy apps returned same Responses ")
  }
}
