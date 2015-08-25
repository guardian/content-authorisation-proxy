package com.gu.subscriptions.cas.bootstrap

import com.gu.subscriptions.cas.service.zuora.ZuoraClient

trait ServiceInitialiser {
  def init() = {
    ZuoraClient.authTask.start()
  }
}
