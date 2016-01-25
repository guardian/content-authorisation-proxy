package com.gu.subscriptions.cas.model

import com.gu.zuora.soap.models.Queries.Contact


object ContactOps {
  private def format(str: String): String = str.filter(_.isLetterOrDigit).toLowerCase

  implicit class WithMatchingPassword(contact: Contact) {
    def matchesPassword(password: String): Boolean = {
      val formattedPwd = format(password)

      Seq(contact.postalCode, Some(contact.lastName)).exists { candidate =>
        candidate.map(format).contains(formattedPwd)
      }
    }
  }
}
