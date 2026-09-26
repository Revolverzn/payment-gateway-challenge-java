package com.checkout.payment.gateway.validation;

import com.checkout.payment.gateway.exception.PaymentValidationException;
import com.checkout.payment.gateway.model.FieldViolation;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.validation.rule.PaymentValidationRule;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Runs every {@link PaymentValidationRule} bean against the request.
 * Spring injects all rule implementations into the ordered list automatically;
 * new rules can be added without modifying this class.
 */
@Component
public class PaymentValidator {

  private final List<PaymentValidationRule> rules;

  public PaymentValidator(List<PaymentValidationRule> rules) {
    this.rules = List.copyOf(rules);
  }

  public void validate(PostPaymentRequest request) {
    List<FieldViolation> violations = new ArrayList<>();

    for (PaymentValidationRule rule : rules) {
      rule.validate(request).ifPresent(violations::add);
    }

    if (!violations.isEmpty()) {
      throw new PaymentValidationException(violations);
    }
  }
}
