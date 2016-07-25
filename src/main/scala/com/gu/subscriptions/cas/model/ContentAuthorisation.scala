package com.gu.subscriptions.cas.model

import com.google.common.base.Charsets.UTF_8
import com.google.common.hash.Hashing

case class Credentials(subscriptionName: Option[String], password: Option[String]) {
  lazy val areSet =
  lazy val get: String =
    Hashing.sha256().newHasher(appId.length + deviceId.length).putString(appId, UTF_8).putString(deviceId, UTF_8).hash.toString

  override def toString = s"$subscriptionName's credentials"
}

case class ContentAuthorisation(appId: String,
                                deviceId: String,
                                expiry: Expiry,
                                credentialsHash: Option[String] = None) {

  lazy val installationId: String =
    Hashing.sha256().newHasher(appId.length + deviceId.length).putString(appId, UTF_8).putString(deviceId, UTF_8).hash.toString

  def withExpiry(newExpiry: Expiry) = ContentAuthorisation(appId, deviceId, newExpiry, credentialsHash)
  def withCredentialsHash(newCredentialsHash: String) = ContentAuthorisation(appId, deviceId, expiry, Some(newCredentialsHash))
}