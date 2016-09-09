package com.gu.subscriptions.cas.model

import com.google.common.base.Charsets.UTF_8
import com.google.common.hash.Hashing

case class Credentials(subscriberId: String, password: String) {
  val areValid: Boolean = subscriberId.nonEmpty && password.nonEmpty
  lazy val hash = Hashing.sha256().newHasher(subscriberId.length + password.length).putString(subscriberId, UTF_8).putString(password, UTF_8).hash.toString
  override def toString = s"$subscriberId's credentials"
}

object Credentials {
  def apply(subscriberId: Option[String], password: Option[String]): Credentials = {
    Credentials(subscriberId.mkString.trim, password.mkString.trim)
  }
}

case class Installation(appId: String, deviceId: String) {
  lazy val hash = Hashing.sha256().newHasher(appId.length + deviceId.length).putString(appId, UTF_8).putString(deviceId, UTF_8).hash.toString
}

object Installation {
  def apply(appId: Option[String], deviceId: Option[String]): Installation = {
    Installation(appId.mkString.trim, deviceId.mkString.trim)
  }
}

case class ContentAuthorisation(installationHash: String,
                                expiry: Expiry,
                                credentialsHash: Option[String] = None) {
}