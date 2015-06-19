package com.gu.subscriptions.cas.bootstrap

import ch.qos.logback.classic.{Logger => LogbackLogger, LoggerContext}
import ch.qos.logback.classic.filter.ThresholdFilter
import com.gu.subscriptions.cas.config.Configuration.{stage, sentryDsn}
import net.kencochrane.raven.RavenFactory
import net.kencochrane.raven.logback.SentryAppender
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory
import app.BuildInfo

object SentryLogging {
  private val log = LoggerFactory.getLogger(getClass)

  def init() =
    sentryDsn match  {
      case Some(dsn) =>
        log.info("Sentry DSN found, we will report errors to Sentry")
        val tags       = Map("stage" -> stage) ++ BuildInfo.toMap
        val tagsString = tags.map { case (key, value) => s"$key:$value"}.mkString(",")

        val filter = new ThresholdFilter { setLevel("ERROR") }
        filter.start()

        val sentryAppender = new SentryAppender(RavenFactory.ravenInstance(dsn)) {
          addFilter(filter)
          setTags(tagsString)
          setContext(LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext])
        }

        sentryAppender.start()

        LoggerFactory.getLogger(ROOT_LOGGER_NAME)
          .asInstanceOf[LogbackLogger]
          .addAppender(sentryAppender)

      case None =>
        if (stage == "PROD") log.error("Setting 'sentry.dsn' is blank! The app will not be able to report errors to Sentry")
    }

}
