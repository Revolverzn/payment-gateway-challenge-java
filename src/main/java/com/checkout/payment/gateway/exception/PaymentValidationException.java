package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.FieldViolation;
import java.util.List;
import java.util.stream.Collectors;

public class PaymentValidationException extends RuntimeException {

  private final transient List<FieldViolation> violations;

  public PaymentValidationException(List<FieldViolation> violations) {
    super(violations.stream()
        .map(violation -> violation.field() + ": " + violation.message())
        .collect(Collectors.joining("; ")));
    this.violations = List.copyOf(violations);
  }

  public List<FieldViolation> getViolations() {
    return violations;
  }
}
