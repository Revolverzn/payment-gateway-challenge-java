package com.checkout.payment.gateway.validation.rule;

import com.checkout.payment.gateway.model.FieldViolation;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import java.time.YearMonth;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class ExpiryDateValidationRule extends PaymentValidationRule {

  private static final String MONTH_FIELD = "expiry_month";
  private static final String YEAR_FIELD = "expiry_year";
  private static final int MIN_YEAR = 1;
  private static final int MAX_YEAR = 9999;

  @Override
  public Optional<FieldViolation> validate(PostPaymentRequest request) {
    int expiryMonth = request.getExpiryMonth();
    int expiryYear = request.getExpiryYear();

    if (expiryMonth < 1 || expiryMonth > 12) {
      return fail(MONTH_FIELD, "must be between 1 and 12");
    }
    if (expiryYear < MIN_YEAR || expiryYear > MAX_YEAR) {
      return fail(YEAR_FIELD, "must be a 4-digit year");
    }

    YearMonth expiry = YearMonth.of(expiryYear, expiryMonth);
    if (!expiry.isAfter(YearMonth.now())) {
      return fail(YEAR_FIELD, "expiry date must be in the future");
    }
    return Optional.empty();
  }
}
