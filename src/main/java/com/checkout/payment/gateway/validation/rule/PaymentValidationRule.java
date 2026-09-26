package com.checkout.payment.gateway.validation.rule;

import com.checkout.payment.gateway.model.FieldViolation;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import java.util.Optional;

/**
 * Base class for a single payment validation rule.
 * Each rule is an independent Spring component; adding a new rule requires
 * no change to {@code PaymentValidator} (open/closed principle).
 */
public abstract class PaymentValidationRule {

  /**
   * Validates one aspect of the payment request.
   *
   * @return a field violation describing the failure, or empty if the rule passes
   */
  public abstract Optional<FieldViolation> validate(PostPaymentRequest request);

  protected Optional<FieldViolation> fail(String field, String message) {
    return Optional.of(new FieldViolation(field, message));
  }

  protected boolean matchesNumericRange(String value, int minLength, int maxLength) {
    return value != null && value.matches("^[0-9]{" + minLength + "," + maxLength + "}$");
  }
}
