package com.gu.subscriptions.cas.directives

import com.gu.subscriptions.CAS.CASError
import com.gu.subscriptions.cas.model.json.ModelJsonProtocol._
import spray.http.MediaTypes.`application/json`
import spray.http.StatusCodes._
import spray.json._
import ResponseCodeTransformer._

import org.scalatest.FreeSpec
import spray.http.{HttpEntity, HttpResponse}

class ResponseCodeTransformerSpec extends FreeSpec {
  "The response code is changed" - {
    def response(code: Int): HttpResponse = {
      val error = CASErrorWrapper(CASError("error message", Some(code)))
      HttpResponse(entity = HttpEntity(`application/json`, error.toJson.toString()))
    }

    "when there is a bad request" in {
      val resp = response(-40)
      assertResult(BadRequest)(changeResponseCode(resp).status)
    }

    "when there is an unauthorized request" in {
      val resp = response(-60)
      assertResult(Unauthorized)(changeResponseCode(resp).status)
    }

    "when there is an unknown error" in {
      val resp = response(100)
      assertResult(InternalServerError)(changeResponseCode(resp).status)
    }
  }

  "The response is not changed when an error cannot be extracted from the entity" in {
    val resp = HttpResponse()
    assertResult(resp)(changeResponseCode(resp))
  }
}
