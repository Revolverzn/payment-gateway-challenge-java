package com.checkout.payment.gateway.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.checkout.payment.gateway.exception.PaymentValidationException;
import com.checkout.payment.gateway.model.FieldViolation;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.validation.rule.AmountValidationRule;
import com.checkout.payment.gateway.validation.rule.CardNumberValidationRule;
import com.checkout.payment.gateway.validation.rule.CurrencyValidationRule;
import com.checkout.payment.gateway.validation.rule.CvvValidationRule;
import com.checkout.payment.gateway.validation.rule.ExpiryDateValidationRule;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentValidatorTest {

  private PaymentValidator validator;

  private static PostPaymentRequest validRequest() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("2222405343248877");
    request.setExpiryMonth(12);
    request.setExpiryYear(2030);
    request.setCurrency("GBP");
    request.setAmount(100);
    request.setCvv(123);
    return request;
  }

  @BeforeEach
  void setUp() {
    validator = new PaymentValidator(List.of(
        new CardNumberValidationRule(),
        new ExpiryDateValidationRule(),
        new CurrencyValidationRule(),
        new AmountValidationRule(),
        new CvvValidationRule()));
  }

  @Test
  void whenRequestIsValidThenNoExceptionThrown() {
    assertDoesNotThrow(() -> validator.validate(validRequest()));
  }

  @Test
  void whenCardNumberTooShortThenValidationFails() {
    PostPaymentRequest request = validRequest();
    request.setCardNumber("123");

    PaymentValidationException ex = assertThrows(PaymentValidationException.class,
        () -> validator.validate(request));
    assertTrue(ex.getMessage().contains("card_number"));
  }

  @Test
  void whenCardNumberNotNumericThenValidationFails() {
    PostPaymentRequest request = validRequest();
    request.setCardNumber("abcd405343248877");

    PaymentValidationException ex = assertThrows(PaymentValidationException.class,
        () -> validator.validate(request));
    assertTrue(ex.getMessage().contains("card_number"));
  }

  @Test
  void whenExpiryMonthOutOfRangeThenValidationFails() {
    PostPaymentRequest request = validRequest();
    request.setExpiryMonth(13);

    PaymentValidationException ex = assertThrows(PaymentValidationException.class,
        () -> validator.validate(request));
    assertTrue(ex.getMessage().contains("expiry_month"));
  }

  @Test
  void whenExpiryYearOutOfRangeThenValidationFails() {
    PostPaymentRequest request = validRequest();
    request.setExpiryYear(10000);

    PaymentValidationException ex = assertThrows(PaymentValidationException.class,
        () -> validator.validate(request));
    assertTrue(ex.getMessage().contains("expiry_year"));
  }

  @Test
  void whenCardExpiredThenValidationFails() {
    PostPaymentRequest request = validRequest();
    request.setExpiryMonth(1);
    request.setExpiryYear(2020);

    PaymentValidationException ex = assertThrows(PaymentValidationException.class,
        () -> validator.validate(request));
    assertTrue(ex.getMessage().contains("expiry date must be in the future"));
  }

  @Test
  void whenCurrencyUnsupportedThenValidationFails() {
    PostPaymentRequest request = validRequest();
    request.setCurrency("JPY");

    PaymentValidationException ex = assertThrows(PaymentValidationException.class,
        () -> validator.validate(request));
    assertTrue(ex.getMessage().contains("currency"));
  }

  @Test
  void whenAmountNotPositiveThenValidationFails() {
    PostPaymentRequest request = validRequest();
    request.setAmount(0);

    PaymentValidationException ex = assertThrows(PaymentValidationException.class,
        () -> validator.validate(request));
    assertTrue(ex.getMessage().contains("amount"));
  }

  @Test
  void whenCvvWrongLengthThenValidationFails() {
    PostPaymentRequest request = validRequest();
    request.setCvv(12);

    PaymentValidationException ex = assertThrows(PaymentValidationException.class,
        () -> validator.validate(request));
    assertTrue(ex.getMessage().contains("cvv"));
  }

  @Test
  void whenMultipleRulesFailThenAllErrorsAreReported() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("123");
    request.setExpiryMonth(13);
    request.setExpiryYear(2020);
    request.setCurrency("JPY");
    request.setAmount(-5);
    request.setCvv(12);

    PaymentValidationException ex = assertThrows(PaymentValidationException.class,
        () -> validator.validate(request));

    // Every violation is reported structurally with its field name, in rule
    // order, so clients can render per-field errors.
    List<String> fields = ex.getViolations().stream()
        .map(FieldViolation::field)
        .collect(Collectors.toList());
    assertEquals(List.of("card_number", "expiry_month", "currency", "amount", "cvv"),
        fields);
    assertTrue(ex.getViolations().stream().allMatch(v -> !v.message().isBlank()));
  }
}
