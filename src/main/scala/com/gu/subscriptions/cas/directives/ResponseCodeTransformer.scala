package com.gu.subscriptions.cas.directives

import com.gu.subscriptions.CAS.CASError
import com.gu.subscriptions.cas.model.json.ModelJsonProtocol._
import spray.http.HttpResponse
import spray.http.StatusCodes._
import spray.json._

import scala.util.Try

object ResponseCodeTransformer {
  /**
   * Change the response code that is returned by the call to CAS.
   * This is necessary because in some cases CAS sends a 200 response
   * containing an error, making the API misleading.
   * The error codes are defined there:
   * https://github.com/guardian/content-authorisation/blob/master/content-authorisation/src/main/resources/conf/global.properties
   */

  val badRequestCodes = Seq(-40, -50, -80)
  case class BadRequestCode(code: Int)
  object BadRequestCode {
    def unapply(code: Int) = if (badRequestCodes.contains(code)) Some(BadRequestCode(code)) else None
  }

  val unauthorizedCodes = Seq(-60, -70, -90, -100, -110, -120)
  case class UnauthorizedCode(code: Int)
  object UnauthorizedCode {
    def unapply(code: Int) = if (unauthorizedCodes.contains(code)) Some(UnauthorizedCode(code)) else None
  }

  val changeResponseCode: HttpResponse => HttpResponse = resp =>
    Try {
      resp.entity.asString.parseJson.convertTo[CASErrorWrapper]
    }.map(_.error).map {
      // Caveat: A malformed json payload send to the /auth endpoint will result in an HTML 404 response by
      // the CAS server so won't be caught in the pattern match below.
      case CASError(_, BadRequestCode(_)) => resp.copy(status = BadRequest)
      case CASError(_, UnauthorizedCode(_)) => resp.copy(status = Unauthorized)
      case _ => resp.copy(status = InternalServerError)
    }.getOrElse(resp)
}
