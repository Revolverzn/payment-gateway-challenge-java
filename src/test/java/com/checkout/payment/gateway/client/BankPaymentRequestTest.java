package com.checkout.payment.gateway.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.junit.jupiter.api.Test;

class BankPaymentRequestTest {

  @Test
  void fromZeroPadsMonthAndSendsCvvAsStringToMatchBankContract() {
    BankPaymentRequest bankRequest = BankPaymentRequest.from(validRequest());

    assertEquals("04/2030", bankRequest.getExpiryDate());
    assertEquals("123", bankRequest.getCvv());
  }

  @Test
  void toStringMasksCardNumberAndHidesCvv() {
    String text = BankPaymentRequest.from(validRequest()).toString();

    assertTrue(text.contains("****8877"));
    assertFalse(text.contains("222240534324"));
    assertTrue(text.contains("cvv='***'"));
  }

  private PostPaymentRequest validRequest() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("2222405343248877");
    request.setExpiryMonth(4);
    request.setExpiryYear(2030);
    request.setCurrency("GBP");
    request.setAmount(100);
    request.setCvv(123);
    return request;
  }
}
