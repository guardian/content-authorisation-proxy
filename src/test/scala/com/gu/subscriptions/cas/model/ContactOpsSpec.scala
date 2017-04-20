package com.gu.subscriptions.cas.model

import com.gu.zuora.soap.models.Queries.Contact
import org.scalatest.{FlatSpec, Matchers}
import ContactOps.WithMatchingPassword

class ContactOpsSpec extends FlatSpec with Matchers {
  behavior of "ContactOps.matchesPassword"

  def contact(postcode: String = "123") = Contact("id", "FirstName", "LastName", Some(postcode), None, None)

  it should "ignore case of either arguments" in {
    contact("N1 9GU").matchesPassword("n1 9gu") should be(true)
    contact("N1 9gu").matchesPassword("N1 9GU") should be(true)
  }

  it should "ignore whitespaces in either arguments" in {
    contact("N19GU").matchesPassword("n1 9gu") should be(true)
    contact("N1 9gu").matchesPassword("N19GU") should be(true)
  }

  it should "not accept essentially different postcodes" in {
    contact("N19GU").matchesPassword("n2 9gu") should be(false)
    contact("N2 9gu").matchesPassword("N19GU") should be(false)
  }

  it should "accept the last name" in {
    contact().matchesPassword("last'- name") should be(true)
  }
}
