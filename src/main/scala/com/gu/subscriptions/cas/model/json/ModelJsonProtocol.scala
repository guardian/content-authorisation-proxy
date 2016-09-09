package com.gu.subscriptions.cas.model.json

import com.gu.subscriptions.CAS.CASError
import com.gu.subscriptions.cas.model._
import spray.json._

object ModelJsonProtocol extends DefaultJsonProtocol {

  implicit val authRequestFormat = jsonFormat3(AuthorisationRequest)
  implicit val subsRequestFormat = jsonFormat5(SubscriptionRequest)

  case class CASErrorWrapper(error: CASError)
  implicit val casErrorFormat = jsonFormat2(CASError)
  implicit val authorisationError = jsonFormat2(AuthorisationError)
  implicit val casErrorWrapperFormat = jsonFormat1(CASErrorWrapper)

  implicit val expiryFormat = jsonFormat3(Expiry)
  implicit val authorisationResponseWrapperFormat = jsonFormat2(AuthorisationResponse)
  implicit val contentAuthorisationWrapperFormat = jsonFormat3(ContentAuthorisation)

}
