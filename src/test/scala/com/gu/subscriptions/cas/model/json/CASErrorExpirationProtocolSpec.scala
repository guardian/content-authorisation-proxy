package com.gu.subscriptions.cas.model.json

import com.gu.cas.CAS.CASError
import org.scalatest.FreeSpec
import spray.json._
import ModelJsonProtocol._

class CASErrorExpirationProtocolSpec extends FreeSpec {
  "For CASError" - {
    val error = CASError("message", -1)
    val json =
      """
        |{ "error": { "message": "message", "code": -1 } }
      """.stripMargin

    "handles serialization" in {
      assertResult(json.parseJson)(error.toJson)
    }

    "handles deserialization" in {
      assertResult(error)(json.parseJson.convertTo[CASError])
    }
  }
}
