package com.checkout.payment.gateway.validation.rule;

import com.checkout.payment.gateway.model.FieldViolation;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(4)
public class AmountValidationRule extends PaymentValidationRule {

  private static final String FIELD = "amount";

  @Override
  public Optional<FieldViolation> validate(PostPaymentRequest request) {
    if (request.getAmount() <= 0) {
      return fail(FIELD, "must be a positive integer");
    }
    return Optional.empty();
  }
}
