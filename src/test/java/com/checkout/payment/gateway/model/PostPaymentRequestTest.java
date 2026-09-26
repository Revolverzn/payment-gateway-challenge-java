package com.checkout.payment.gateway.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PostPaymentRequestTest {

  @Test
  void toStringMasksCardNumberAndHidesCvv() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("2222405343248877");
    request.setCvv(123);

    String text = request.toString();

    assertTrue(text.contains("****8877"));
    assertFalse(text.contains("222240534324"));
    assertTrue(text.contains("cvv='***'"));
    assertFalse(text.contains("123"));
  }

  @Test
  void toStringHandlesMissingOrShortCardNumber() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber(null);
    assertTrue(request.toString().contains("****"));

    request.setCardNumber("12");
    assertTrue(request.toString().contains("****"));
  }
}
