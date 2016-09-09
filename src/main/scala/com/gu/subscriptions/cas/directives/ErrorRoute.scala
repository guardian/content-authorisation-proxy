package com.gu.subscriptions.cas.directives

import com.gu.subscriptions.cas.config.Configuration.{system, _}
import com.gu.subscriptions.cas.model.{AuthorisationError, AuthorisationResponse}
import spray.http.{HttpResponse, StatusCodes}
import spray.json.{JsNumber, JsObject, JsString}
import spray.routing.Route
import spray.routing.directives.RouteDirectives.complete

trait ErrorRoute {
  def errorMsg(msg: String, code: Int): AuthorisationResponse = AuthorisationResponse(None, Some(AuthorisationError(msg, code)))

  val badRequest: Route =
    complete(
      HttpResponse(
        status = StatusCodes.BadRequest,
        entity = errorMsg("Mandatory data missing from request", mandatoryDataMissingErrorCode)
      )
    )

  val notFound: Route =
    complete(
      HttpResponse(
        status = StatusCodes.NotFound,
        entity = errorMsg("Unknown subscriber", unknownSubscriberErrorCode)
      )
    )

  val serverError: Route =
    complete(
      HttpResponse(
        status = StatusCodes.InternalServerError,
        entity = errorMsg("Internal server error", generalErrorCode)
      )
    )

  val serviceUnavailableError: Route =
    complete(
      HttpResponse(
        status = StatusCodes.ServiceUnavailable,
        entity = errorMsg("Service Unavailable. Please try again later", networkErrorCode)
      )
    )

  val tooManyRegistrationsError: Route =
    complete(
      HttpResponse(
        status = StatusCodes.Forbidden,
        entity = errorMsg("Credentials used too often", credentialsOverUseErrorCode)
      )
    )
}
