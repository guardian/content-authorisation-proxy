package com.gu.subscriptions.cas.service.api

import org.joda.time.{LocalDate, ReadablePeriod}

import scala.concurrent.Future


sealed trait GetExpirationResponse

sealed trait SetExpirationResponse

case class Success(expiration: Option[LocalDate]) extends GetExpirationResponse

case class Error(message: String) extends GetExpirationResponse with SetExpirationResponse

object Success extends SetExpirationResponse


trait DataStore {
  def getExpiration(appId: String, deviceId: String): Future[GetExpirationResponse]

  def setExpiration(appId: String, deviceId: String, expiration: LocalDate, timeToLive: ReadablePeriod): Future[SetExpirationResponse]
}
