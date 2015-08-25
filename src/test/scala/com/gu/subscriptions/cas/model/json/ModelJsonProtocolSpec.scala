package com.gu.subscriptions.cas.model.json

import com.gu.cas.CAS.CASError
import com.gu.subscriptions.cas.model.SubscriptionExpiration
import org.joda.time.DateTime
import org.scalatest.FreeSpec
import spray.json._
import ModelJsonProtocol._

class ModelJsonProtocolSpec extends FreeSpec {
  "For CASError" - {
    val error = CASErrorWrapper(CASError("message", -1))
    val json =
      """
        |{ "error": { "message": "message", "code": -1 } }
      """.stripMargin

    "handles serialization" in {
      assertResult(json.parseJson)(error.toJson)
    }

    "handles deserialization" in {
      assertResult(error)(json.parseJson.convertTo[CASErrorWrapper])
    }
  }

  "For SubscriptionExpiration" - {
    "handles serialization" in {
      val expiration = SubscriptionExpiration(
        expiryDate = new DateTime(2015, 5, 21, 12, 0)
      )

      val expected =
        """
          |{
          |  "expiry": {
          |    "expiryDate": "2015-05-21",
          |    "expiryType": "sub",
          |    "content": "SevenDay"
          |  }
          |}
        """.stripMargin

      assertResult(expected.parseJson)(expiration.toJson)
    }
  }
}
