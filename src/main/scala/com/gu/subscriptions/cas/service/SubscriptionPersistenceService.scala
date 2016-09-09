package com.gu.subscriptions.cas.service

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.amazonaws.services.dynamodbv2.model.PutItemResult
import com.gu.scanamo.syntax._
import com.gu.scanamo.{ScanamoAsync, Table}
import com.gu.subscriptions.cas.config.Configuration.dynamoClient
import com.gu.subscriptions.cas.model.ContentAuthorisation
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future

trait SubscriptionPersistenceService {
  def get(installationId: String): Future[Option[ContentAuthorisation]]
  def set(contentAuthorisation: ContentAuthorisation): Future[PutItemResult]
  def getAll(subscriberId: String): Future[List[ContentAuthorisation]]
}

class ScanamoCASService(client: AmazonDynamoDBAsyncClient, table: String) extends SubscriptionPersistenceService with LazyLogging {
  val scanamo = Table[ContentAuthorisation](table)
  val index = scanamo.index("subscriberId-index")

  def run[T] = ScanamoAsync.exec[T](client) _

  override def get(installationId: String) =
    run(scanamo.get('installationId -> installationId).map(_.flatMap(_.toOption)))

  override def set(contentAuthorisation: ContentAuthorisation) = run(scanamo.put(contentAuthorisation))

  override def getAll(subscriberId: String) = run(index.query('subscriberId -> subscriberId).map(_.flatMap(_.toOption)))
}

object SubscriptionPersistenceService {
  lazy val fromConfig = new ScanamoCASService(dynamoClient, "ContentAuthorisations")
}