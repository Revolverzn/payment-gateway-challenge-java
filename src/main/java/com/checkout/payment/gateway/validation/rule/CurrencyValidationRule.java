package com.checkout.payment.gateway.validation.rule;

import com.checkout.payment.gateway.model.FieldViolation;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class CurrencyValidationRule extends PaymentValidationRule {

  private static final String FIELD = "currency";
  private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "GBP", "EUR");

  @Override
  public Optional<FieldViolation> validate(PostPaymentRequest request) {
    String currency = request.getCurrency();
    if (currency == null || currency.isEmpty()
        || !SUPPORTED_CURRENCIES.contains(currency)) {
      return fail(FIELD, "must be one of " + SUPPORTED_CURRENCIES);
    }
    return Optional.empty();
  }
}
