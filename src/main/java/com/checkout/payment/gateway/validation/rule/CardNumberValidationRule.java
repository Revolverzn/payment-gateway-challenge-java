package com.checkout.payment.gateway.validation.rule;

import com.checkout.payment.gateway.model.FieldViolation;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class CardNumberValidationRule extends PaymentValidationRule {

  private static final String FIELD = "card_number";
  private static final int MIN_LENGTH = 14;
  private static final int MAX_LENGTH = 19;

  @Override
  public Optional<FieldViolation> validate(PostPaymentRequest request) {
    if (!matchesNumericRange(request.getCardNumber(), MIN_LENGTH, MAX_LENGTH)) {
      return fail(FIELD, "must be between " + MIN_LENGTH + " and " + MAX_LENGTH
          + " numeric characters");
    }
    return Optional.empty();
  }
}
