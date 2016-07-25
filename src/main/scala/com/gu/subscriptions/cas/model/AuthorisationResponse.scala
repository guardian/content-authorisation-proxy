package com.gu.subscriptions.cas.model

case class AuthorisationResponse(expiry: Option[Expiry], error: Option[AuthorisationError] = None)
