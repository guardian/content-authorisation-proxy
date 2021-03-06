package com.gu.subscriptions.cas.directives

import com.gu.subscriptions.cas.config.Configuration.{system, _}
import spray.http.{HttpResponse, StatusCodes}
import spray.json.{JsObject, JsString, JsNumber}
import spray.routing.Route
import spray.routing.directives.RouteDirectives.complete

trait ErrorRoute {
  def errorMsg(msg: String, code: Int): String =
    JsObject("error" -> JsObject("message" -> JsString(msg), "code" -> JsNumber(code))).toString()

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
  val serverErrorResponse = HttpResponse(
    status = StatusCodes.InternalServerError,
    entity = errorMsg("Internal server error", generalErrorCode)
  )

  val serverError: Route =
    complete(
    serverErrorResponse
    )

  val serviceUnavailableError: Route =
    complete(
      HttpResponse(
        status = StatusCodes.ServiceUnavailable,
        entity = errorMsg("Service Unavailable. Please try again later", networkErrorCode)
      )
    )
}
