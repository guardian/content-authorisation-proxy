package com.gu.subscriptions.cas.service.api

import org.joda.time.DateTime

import scala.concurrent.Future


sealed trait GetExpirationResponse

sealed trait SetExpirationResponse

case class Success(expiration: Option[DateTime]) extends GetExpirationResponse

case class Error(message: String) extends GetExpirationResponse with SetExpirationResponse

object Success extends SetExpirationResponse


trait DataStore {
  def getExpiration(appId: String, deviceId: String): Future[GetExpirationResponse]

  def setExpiration(appId: String, deviceId: String, expiration: DateTime): Future[SetExpirationResponse]
}
