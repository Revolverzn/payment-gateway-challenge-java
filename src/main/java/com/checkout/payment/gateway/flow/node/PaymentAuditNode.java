package com.checkout.payment.gateway.flow.node;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.flow.PaymentProcessContext;
import com.yomahub.liteflow.core.NodeComponent;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Chain node 8: audits the completed payment before it leaves the chain.
 * Verifies the response reached a terminal status (never ship an INIT
 * result as success), writes an audit log entry and counts the terminal
 * business outcome (exposed as {@code payments_processed_total}).
 */
@Component("auditPayment")
public class PaymentAuditNode extends NodeComponent {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentAuditNode.class);

  private final MeterRegistry meterRegistry;

  public PaymentAuditNode(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Override
  public void process() {
    PaymentProcessContext context = getContextBean(PaymentProcessContext.class);
    if (context.getPaymentResponse() == null
        || context.getPaymentResponse().getStatus() == PaymentStatus.INIT) {
      throw new IllegalStateException("Payment did not reach a terminal status");
    }
    LOG.info("Payment {} processed with status {}, authorization_code {}",
        context.getPaymentResponse().getId(), context.getPaymentStatus(),context.getAuthorizationCode());
    // Replayed requests short-circuit before this node, so this counts only
    // payments that were actually sent to the bank.
    meterRegistry.counter("payments.processed",
        "status", context.getPaymentStatus().name()).increment();
  }
}
