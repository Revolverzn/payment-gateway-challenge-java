package com.checkout.payment.gateway.validation.rule;

import com.checkout.payment.gateway.model.FieldViolation;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(5)
public class CvvValidationRule extends PaymentValidationRule {

  private static final String FIELD = "cvv";
  private static final int MIN_LENGTH = 3;
  private static final int MAX_LENGTH = 4;

  @Override
  public Optional<FieldViolation> validate(PostPaymentRequest request) {
    String cvv = String.valueOf(request.getCvv());
    if (!matchesNumericRange(cvv, MIN_LENGTH, MAX_LENGTH)) {
      return fail(FIELD, "must be between " + MIN_LENGTH + " and " + MAX_LENGTH
          + " numeric characters");
    }
    return Optional.empty();
  }
}
