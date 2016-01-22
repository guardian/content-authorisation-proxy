package com.gu.subscriptions.cas.model

import com.gu.zuora.soap.models.Queries.Contact


object Implicits {
  implicit class ContactOpts(contact: Contact) {
    def samePostcode(otherPostalCode: String): Boolean = {
      def format(pc: String): String = pc.replaceAll("\\s+", "").toLowerCase

      contact.postalCode.exists(pc => format(pc) == format(otherPostalCode))
    }
  }
}
