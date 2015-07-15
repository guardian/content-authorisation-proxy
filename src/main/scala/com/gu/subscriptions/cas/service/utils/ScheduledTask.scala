package com.gu.subscriptions.cas.service.utils

import akka.actor.ActorSystem
import akka.agent.Agent
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait ScheduledTask[T] {
  val initialValue: T

  val initialDelay: FiniteDuration
  val interval: FiniteDuration

  val name = getClass.getSimpleName

  private implicit val system = ActorSystem("scheduled-task")
  lazy val agent = Agent[T](initialValue)

  def refresh(): Future[T]

  def start() {
    Logger.debug(s"Starting $name scheduled task")
    system.scheduler.schedule(initialDelay, interval) {
      agent.sendOff { _ =>
        Logger.debug(s"Refreshing $name scheduled task")
        Await.result(refresh(), 25.seconds)
      }
    }
  }

  def get() = agent.get()
}

object ScheduledTask {
  def apply[T](taskName: String, initValue: T, initDelay: FiniteDuration, intervalPeriod: FiniteDuration)(refresher: => Future[T]) =
    new ScheduledTask[T] {
      val initialValue = initValue
      val initialDelay = initDelay
      val interval = intervalPeriod

      override val name = taskName

      def refresh(): Future[T] = refresher
    }
}