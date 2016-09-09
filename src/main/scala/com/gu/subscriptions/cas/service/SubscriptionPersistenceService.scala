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
  def get(installationHash: String): Future[Option[ContentAuthorisation]]
  def set(contentAuthorisation: ContentAuthorisation): Future[PutItemResult]
  def getAll(credentialsHash: String): Future[List[ContentAuthorisation]]
}

class ScanamoCASService(client: AmazonDynamoDBAsyncClient, table: String) extends SubscriptionPersistenceService with LazyLogging {
  import scala.concurrent.ExecutionContext.Implicits.global

  val scanamo = Table[ContentAuthorisation](table)
  val index = scanamo.index("credentialsHash-index")

  def run[T] = ScanamoAsync.exec[T](client) _

  override def get(installationHash: String) =
    run(scanamo.get('installationHash -> installationHash).map(_.flatMap(_.toOption)))

  override def set(contentAuthorisation: ContentAuthorisation) = run(scanamo.put(contentAuthorisation))

  override def getAll(credentialsHash: String) = run(index.query('credentialsHash -> credentialsHash).map(_.flatMap(_.toOption)))
}

object SubscriptionPersistenceService {
  lazy val fromConfig = new ScanamoCASService(dynamoClient, "ContentAuthorisations_DEV")
}