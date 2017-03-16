package com.gu.subscriptions.cas.model.json

import com.gu.subscriptions.CAS.CASError
import com.gu.subscriptions.cas.model.{ExpiryType, SubscriptionExpiration}
import org.joda.time.DateTime
import org.scalatest.FreeSpec
import spray.json._
import ModelJsonProtocol._
import com.gu.cas.SevenDay


class ModelJsonProtocolSpec extends FreeSpec {
  "For CASError" - {
    val error = CASErrorWrapper(CASError("message", Some(-1)))
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
        expiryDate = new DateTime(2015, 5, 21, 12, 0),
        expiryType = ExpiryType.SUB
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
    "handles serialization of emergency token response" in {
      val expiration = SubscriptionExpiration(
        expiryDate = new DateTime(2015, 5, 21, 12, 0),
        expiryType = ExpiryType.SUB,
        provider = Some("G99"),
        subscriptionCode = Some(SevenDay)
      )

      val expected =
        """
          |{
          |  "expiry": {
          |    "expiryDate": "2015-05-21",
          |    "expiryType": "sub",
          |    "provider": "G99",
          |    "content": "SevenDay",
          |    "subscriptionCode": "SevenDay"
          |  }
          |}
        """.stripMargin

      assertResult(expected.parseJson)(expiration.toJson)
    }
}
