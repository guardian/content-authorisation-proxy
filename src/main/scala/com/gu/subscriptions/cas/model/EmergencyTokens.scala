package com.gu.subscriptions.cas.model

import com.gu.cas.PrefixedTokens

class EmergencyTokens(val prefix: String, secret: String) {
  val decoder = PrefixedTokens(secretKey = secret, emergencySubscriberAuthPrefix = prefix)
}
