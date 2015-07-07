package com.gu.subscriptions.cas.bootstrap

import com.gu.subscriptions.cas.service.ZuoraClient

trait EagerInitialiser {
  def start() = {
    ZuoraClient.authTask.start()
  }
}
