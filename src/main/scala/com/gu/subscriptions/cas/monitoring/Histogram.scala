package com.gu.subscriptions.cas.monitoring

import java.util.UUID

import com.google.common.cache.CacheBuilder
import java.util.concurrent.atomic.LongAdder

import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging

import collection.JavaConversions.mapAsScalaMap
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * This class representes a histogram or frequency-map of strings. It is up to the callee to call count with a string.
  * The getTop method can be called to see the existing values in the cache. There is thus a reporting maximimum of Integer.MAX.
  * The class will log to the attached logger, a count of the top 50 requested strings every hour. Hence it requires an implicit ActorSystem.
  *
  * @param name the name of this histogram, default: a UUID
  * @param expire the duration to expire the histogram entries. Default: 1
  * @param duration the time unit to expire the histogram entries. Default: HOURS
  * @param ec an implicit ExecutionContext required for the scheduled logger
  */
class Histogram(name: String = UUID.randomUUID().toString, expire: Int = 1, duration: TimeUnit = HOURS)(implicit ec: ExecutionContext) extends LazyLogging {

  private val countCache = CacheBuilder.newBuilder.expireAfterWrite(expire, duration).build[String, LongAdder]

  val actorSystem = ActorSystem(s"histogram-logger-$name")

  actorSystem.scheduler.schedule(0.seconds, 1.hour) {
    logger.info(s"""`$name` cache stats:
${countCache.size} entries.
Top-50 requested IDs:
- ${getTop(50).map(e => s"${e._1}: ${e._2}").mkString("\n- ")}
End of report for `$name`""")
  }

  def count(key: String) =
    if (!key.isEmpty) {
      Option(countCache.getIfPresent(key)).fold {
        val counter = new LongAdder
        counter.increment()
        countCache.put(key, counter)
      } {
        _.increment()
      }
    }

  def getTop(amount: Int) = countCache.asMap().filter(_._2.longValue() > 3).toSeq.sortBy(_._2.longValue()).reverse.take(amount)

}
