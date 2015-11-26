package com.gu.subscriptions.cas.model

import com.typesafe.scalalogging.LazyLogging
import spray.http.HttpResponse

object ResponseComparer extends LazyLogging {
  def compare(response: HttpResponse, other: HttpResponse): Unit = {
    val sanitize = (in:String) => in.replaceAll(" ", "").replaceAll("\n", "")
    val responseBody: String = response.entity.asString
    val otherResponseBody: String = other.entity.asString

    def isSame = {
      val hasSameStatus = response.status == other.status
      val hasSameBody = sanitize(responseBody) == sanitize(otherResponseBody)
      hasSameBody && hasSameStatus
    }
    if (!isSame) {
      logger.error("Legacy apps returned different Responses")
      logger.error(responseBody)
      logger.error(otherResponseBody)
    }
    else
      logger.info("Legacy apps returned same Responses ")
  }
}
