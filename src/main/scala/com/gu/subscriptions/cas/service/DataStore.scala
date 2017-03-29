package com.gu.subscriptions.cas.service

import com.amazonaws.auth.{AWSCredentialsProviderChain, DefaultAWSCredentialsProviderChain}
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder
import com.gu.scanamo.DynamoFormat
import com.gu.subscriptions.cas.service.api.{Error, GetExpirationResponse, SetExpirationResponse, Success}
import org.joda.time.{DateTime, DateTimeZone, LocalDate, ReadablePeriod}
import com.gu.scanamo._
import com.gu.scanamo.syntax._
import com.gu.subscriptions.cas.config.Configuration

import scala.concurrent.{ExecutionContext, Future}

class DataStore(implicit ec: ExecutionContext) extends api.DataStore {

  case class AuthItem(appId: String, deviceId: String, expiryDate: LocalDate, ttlTimestamp: Option[Long])

  private lazy val credentialsProvider = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("membership"),
    new DefaultAWSCredentialsProviderChain()
  )

  val dynamoClient = AmazonDynamoDBAsyncClientBuilder.standard()
    .withRegion(Regions.EU_WEST_1)
    .withCredentials(credentialsProvider)
    .build()

  implicit val jodaStringFormat = DynamoFormat.coercedXmap[LocalDate, String, IllegalArgumentException](
    LocalDate.parse(_)
  )(
    _.toString
  )

  val authTable = Table[AuthItem](Configuration.dynamoAuthTableName)

  override def getExpiration(appId: String, deviceId: String): Future[GetExpirationResponse] = {

    val scanamoResponse = ScanamoAsync.exec(dynamoClient)(authTable.get('appId -> appId and 'deviceId -> deviceId))

    scanamoResponse map {
      case Some(Right(item)) => Success(Some(item.expiryDate))
      case Some(Left(e)) => Error(e.toString)
      case None => Success(None)
    }
  }

  override def setExpiration(appId: String, deviceId: String, expiration: LocalDate, timeToLive: ReadablePeriod): Future[SetExpirationResponse] = {

    val ttlTimestamp = DateTime.now.plus(timeToLive).getMillis
    val newItem = AuthItem(appId, deviceId, expiration, Some(ttlTimestamp))

    ScanamoAsync.exec(dynamoClient)(authTable.put(newItem)).map(a => api.Success)
  }
}
