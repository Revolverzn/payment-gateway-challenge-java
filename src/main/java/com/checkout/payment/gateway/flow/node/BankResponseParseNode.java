package com.checkout.payment.gateway.flow.node;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.flow.PaymentProcessContext;
import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * Chain node 6: reads the raw bank response exactly once and puts the parsed
 * business values (status and authorization code) onto the context. Later
 * nodes use those flat fields and never touch the bank DTO.
 */
@Component("parseBankResponse")
public class BankResponseParseNode extends NodeComponent {

  @Override
  public void process() {
    PaymentProcessContext context = getContextBean(PaymentProcessContext.class);
    context.setPaymentStatus(
        PaymentStatus.fromAuthorized(context.getBankResponse().isAuthorized()));
    // The bank returns an empty string (never a code) on a decline; normalise
    // it to null so the API omits the field instead of returning "".
    String authorizationCode = context.getBankResponse().getAuthorizationCode();
    context.setAuthorizationCode(
        authorizationCode == null || authorizationCode.isBlank() ? null : authorizationCode);
  }
}
