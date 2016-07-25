package com.gu.subscriptions.cas.service

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.amazonaws.services.dynamodbv2.model.{PutItemResult, QueryResult, ScanResult}
import com.gu.scanamo.{Index, ScanamoAsync, Table}
import com.gu.subscriptions.cas.model.{ContentAuthorisation, Expiry}
import com.typesafe.scalalogging.LazyLogging
import com.gu.scanamo.syntax._
import com.gu.scanamo.Index._
import com.gu.subscriptions.cas.config.Configuration.dynamoClient

import scala.concurrent.Future

trait SubscriptionPersistenceService {
  def get(primaryKey: String): Future[Option[ContentAuthorisation]]
  def set(data: ContentAuthorisation): Future[PutItemResult]
  def countInstallations(credentialsHash: String): Future[Int]
}

class ScanamoCASService(client: AmazonDynamoDBAsyncClient, table: String) extends SubscriptionPersistenceService with LazyLogging {
  val scanamo = Table[ContentAuthorisation](table)
  val index = scanamo.index("credentialsHash-index")

  def run[T] = ScanamoAsync.exec[T](client) _

  override def get(primaryKey: String) =
    run(scanamo.get('installationId -> primaryKey).map(_.flatMap(_.toOption)))

  override def set(data: ContentAuthorisation) = run(scanamo.put(data))

  override def countInstallations(credentialsHash: String) = Future.successful(1)
}

object SubscriptionPersistenceService {
  lazy val fromConfig = new ScanamoCASService(dynamoClient, "ContentAuthorisations")
}