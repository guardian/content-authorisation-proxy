package com.gu.subscriptions.cas.model

object Provider {
  val TOUCHPOINT = "touchpoint"
  val DEFAULT = "default" // (i.e. CAS-proxy, me, this service)
  val DEVICE_CONFIGURED = "deviceConfigured"
  val SPECIAL = "special" // deprecated - used to be for list of hardcoded app IDs
}