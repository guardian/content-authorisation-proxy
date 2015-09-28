package com.gu.subscriptions.cas.model

import com.gu.membership.zuora.soap.models.Queries.Contact


object Implicits {
  implicit class ContactOpts(contact: Contact) {
    def samePostcode(otherPostalCode: String): Boolean = {
      def format(pc: String): String = pc.replaceAll("\\s+", "").toLowerCase

      format(contact.postalCode) == format(otherPostalCode)
    }
  }
}
