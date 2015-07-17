package com.gu.subscriptions.cas.directives

import spray.http.{HttpResponse, StatusCodes}
import spray.json.{JsString, JsObject}
import spray.routing.Route
import spray.routing.directives.RouteDirectives.complete

trait ErrorRoute {
  def errorMsg(msg: String): String =
    JsObject("error" -> JsObject("message" -> JsString(msg))).toString()

  val badRequest: Route =
    complete(
      HttpResponse(
        status = StatusCodes.BadRequest,
        entity = errorMsg("Bad request")
      )
    )

  val notFound: Route =
    complete(
      HttpResponse(
        status = StatusCodes.NotFound,
        entity = errorMsg("Not found")
      )
    )

  val serverError: Route =
    complete(
      HttpResponse(
        status = StatusCodes.InternalServerError,
        entity = errorMsg("Internal server error")
      )
    )
}
