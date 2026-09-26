package com.checkout.payment.gateway.model;

import java.util.List;

/**
 * Body of a 400 validation failure: every rejected field is reported in one
 * response instead of failing fast on the first violation.
 */
public class ValidationErrorResponse {

  private final List<FieldViolation> errors;

  public ValidationErrorResponse(List<FieldViolation> errors) {
    this.errors = List.copyOf(errors);
  }

  public List<FieldViolation> getErrors() {
    return errors;
  }
}
