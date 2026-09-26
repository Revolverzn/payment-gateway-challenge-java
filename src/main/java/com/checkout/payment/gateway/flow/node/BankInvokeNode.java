package com.checkout.payment.gateway.flow.node;

import com.checkout.payment.gateway.client.BankResponse;
import com.checkout.payment.gateway.client.AcquiringBankClient;
import com.checkout.payment.gateway.flow.PaymentProcessContext;
import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * Chain node 5: invokes the acquiring bank over HTTP. The INIT record
 * already exists, so a timeout/crash here leaves a traceable in-doubt payment.
 */
@Component("invokeBank")
public class BankInvokeNode extends NodeComponent {

  private final AcquiringBankClient bankClient;

  public BankInvokeNode(AcquiringBankClient bankClient) {
    this.bankClient = bankClient;
  }

  @Override
  public void process() {
    PaymentProcessContext context = getContextBean(PaymentProcessContext.class);
    BankResponse bankResponse = bankClient.invoke(context.getBankRequest());
    context.setBankResponse(bankResponse);
  }
}
