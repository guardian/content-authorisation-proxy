package com.gu.subscriptions.cas.model

import com.gu.membership.zuora.soap.models.Query.Contact
import org.scalatest.{FlatSpec, Matchers}
import Implicits.ContactOpts

class ImplicitsSpec extends FlatSpec with Matchers {
  behavior of "ContactOps.samePostcode"
  def contact(postcode: String) = Contact("id", "FirstName", "LastName", postcode)

  it should "ignore case of either arguments" in {
    contact("N1 9GU").samePostcode("n1 9gu") should be(true)
    contact("N1 9gu").samePostcode("N1 9GU") should be(true)
  }

  it should "ignore whitespaces in either arguments" in {
    contact("N19GU").samePostcode("n1 9gu") should be(true)
    contact("N1 9gu").samePostcode("N19GU") should be(true)
  }

  it should "not accept essentially different postcodes" in {
    contact("N19GU").samePostcode("n2 9gu") should be(false)
    contact("N2 9gu").samePostcode("N19GU") should be(false)
  }
}
