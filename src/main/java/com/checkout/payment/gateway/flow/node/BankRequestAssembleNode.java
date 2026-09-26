package com.checkout.payment.gateway.flow.node;

import com.checkout.payment.gateway.client.BankPaymentRequest;
import com.checkout.payment.gateway.flow.PaymentProcessContext;
import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * Chain node 3: assembles the acquiring bank request payload
 * from the validated inbound request.
 */
@Component("assembleBankRequest")
public class BankRequestAssembleNode extends NodeComponent {

  @Override
  public void process() {
    PaymentProcessContext context = getContextBean(PaymentProcessContext.class);
    BankPaymentRequest bankRequest = BankPaymentRequest.from(context.getPaymentRequest());
    context.setBankRequest(bankRequest);
  }
}
