package com.gu.subscriptions.cas.model

import com.gu.salesforce.{Contact => SalesforceContact }
import com.gu.zuora.soap.models.Queries.{Contact => ZuoraContact}

object ContactOps {
  private def format(str: String): String = str.filter(_.isLetterOrDigit).toLowerCase

  implicit class ZuoraContactWithMatchingPassword(contact: ZuoraContact) {
    def matchesPassword(password: String): Boolean = {
      val formattedPwd = format(password)
      Seq(contact.postalCode, Some(contact.lastName)).exists { candidate =>
        candidate.map(format).contains(formattedPwd)
      }
    }
  }

  implicit class SalesforceContactWithMatchingPassword(contact: SalesforceContact) {
    def matchesPassword(password: String): Boolean = {
      val formattedPwd = format(password)
      Set(format(contact.mailingPostcode), format(contact.lastName)).contains(formattedPwd)
    }
  }
}
