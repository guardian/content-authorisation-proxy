package com.gu.subscriptions.cas.model

import com.gu.cas.{Guardian, SevenDay, SubscriptionCode, TokenPayload}


object TokenPayloadOps {

  implicit class TokenPayloadOps(payload: TokenPayload) {
    def expiryDate = payload.creationDate.plus(payload.period).plusDays(1)
  }

  implicit class SubscriptionCodeOps(subCode: SubscriptionCode) {
    def asString = subCode match {
      case SevenDay => "SevenDay"
      case Guardian => "Guardian"
    }
  }

}
