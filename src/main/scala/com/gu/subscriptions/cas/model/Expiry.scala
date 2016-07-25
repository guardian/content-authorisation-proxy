package com.gu.subscriptions.cas.model

case class Expiry(expiryDate: String, expiryType: String, provider: String = Provider.TOUCHPOINT) {
  val content: String = "SevenDay"  // all other content values have been deprecated - premium digital content is available 24/7 or not at all.
}