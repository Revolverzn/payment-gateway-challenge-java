package com.checkout.payment.gateway.flow.node;

import com.checkout.payment.gateway.flow.PaymentProcessContext;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * Chain node 7: applies the parsed bank outcome to the previously initiated
 * INIT record (terminal status and acquirer authorization code) and writes
 * it back.
 */
@Component("updatePaymentOrder")
public class PaymentFinalizeNode extends NodeComponent {

  private final PaymentsRepository paymentsRepository;

  public PaymentFinalizeNode(PaymentsRepository paymentsRepository) {
    this.paymentsRepository = paymentsRepository;
  }

  @Override
  public void process() {
    PaymentProcessContext context = getContextBean(PaymentProcessContext.class);
    context.getPaymentResponse().setStatus(context.getPaymentStatus());
    context.getPaymentResponse().setAuthorizationCode(context.getAuthorizationCode());
    paymentsRepository.save(context.getPaymentResponse());
  }
}
