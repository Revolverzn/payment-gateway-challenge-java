package com.checkout.payment.gateway.flow.node;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.flow.PaymentProcessContext;
import com.yomahub.liteflow.core.NodeComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Chain node 8: audits the completed payment before it leaves the chain.
 * Verifies the response reached a terminal status (never ship an INIT
 * result as success) and writes an audit log entry.
 */
@Component("auditPayment")
public class PaymentAuditNode extends NodeComponent {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentAuditNode.class);

  @Override
  public void process() {
    PaymentProcessContext context = getContextBean(PaymentProcessContext.class);
    if (context.getPaymentResponse() == null
        || context.getPaymentResponse().getStatus() == PaymentStatus.INIT) {
      throw new IllegalStateException("Payment did not reach a terminal status");
    }
    LOG.info("Payment {} processed with status {}, authorization_code {}",
        context.getPaymentResponse().getId(), context.getPaymentStatus(),context.getAuthorizationCode());
  }
}
