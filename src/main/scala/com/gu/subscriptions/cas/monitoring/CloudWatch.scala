package com.gu.subscriptions.cas.monitoring

import java.util.concurrent.Future

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.regions.{Region, ServiceAbbreviations}
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient
import com.amazonaws.services.cloudwatch.model.{Dimension, MetricDatum, PutMetricDataRequest}
import com.typesafe.scalalogging.LazyLogging

trait CloudWatch extends LazyLogging  {
  val region: Region
  val stage: String
  val application : String

  lazy val stageDimension = new Dimension().withName("Stage").withValue(stage)
  def mandatoryDimensions:Seq[Dimension] = Seq(stageDimension)

  lazy val cloudwatch = {
    val client = new AmazonCloudWatchAsyncClient(new DefaultAWSCredentialsProviderChain)
    client.setEndpoint(region.getServiceEndpoint(ServiceAbbreviations.CloudWatch))
    client
  }

  trait LoggingAsyncHandler extends AsyncHandler[PutMetricDataRequest, Void]
  {
    def onError(exception: Exception)
    {
      logger.info(s"CloudWatch PutMetricDataRequest error: ${exception.getMessage}}")
    }
    def onSuccess(request: PutMetricDataRequest, result: Void )
    {
      logger.trace("CloudWatch PutMetricDataRequest - success")
      CloudWatchHealth.hasPushedMetricSuccessfully = true
    }
  }

  object LoggingAsyncHandler extends LoggingAsyncHandler


  def put(name : String, count: Double, extraDimensions: Dimension*): Future[Void] = {
    val metric =
      new MetricDatum()
        .withValue(count)
        .withMetricName(name)
        .withUnit("Count")
        .withDimensions((mandatoryDimensions ++ extraDimensions): _*)

    val request = new PutMetricDataRequest().
      withNamespace(application).withMetricData(metric)

    cloudwatch.putMetricDataAsync(request, LoggingAsyncHandler)
  }
}

object CloudWatchHealth {
  var hasPushedMetricSuccessfully = false
}