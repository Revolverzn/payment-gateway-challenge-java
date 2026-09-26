package com.checkout.payment.gateway.flow.node;

import com.checkout.payment.gateway.flow.PaymentProcessContext;
import com.checkout.payment.gateway.validation.PaymentValidator;
import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * Chain node 1: rejects the request before any external call
 * if any payment field is invalid.
 */
@Component("validatePayment")
public class PaymentValidateNode extends NodeComponent {

  private final PaymentValidator validator;

  public PaymentValidateNode(PaymentValidator validator) {
    this.validator = validator;
  }

  @Override
  public void process() {
    PaymentProcessContext context = getContextBean(PaymentProcessContext.class);
    validator.validate(context.getPaymentRequest());
  }
}
