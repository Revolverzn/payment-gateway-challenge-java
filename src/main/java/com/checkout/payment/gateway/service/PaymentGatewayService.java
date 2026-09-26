package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.flow.PaymentProcessContext;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);
  private static final String PAYMENT_PROCESS_CHAIN = "paymentProcessChain";

  private final PaymentsRepository paymentsRepository;
  private final FlowExecutor flowExecutor;

  public PaymentGatewayService(PaymentsRepository paymentsRepository, FlowExecutor flowExecutor) {
    this.paymentsRepository = paymentsRepository;
    this.flowExecutor = flowExecutor;
  }

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to to payment with ID {}", id);
    return paymentsRepository.get(id)
        .orElseThrow(() -> new EventProcessingException("Payment not found with id " + id));
  }

  public PostPaymentResponse processPayment(PostPaymentRequest paymentRequest,
      String idempotencyKey) {
    PaymentProcessContext context = new PaymentProcessContext(paymentRequest, idempotencyKey);
    LiteflowResponse response = flowExecutor.execute2Resp(
        PAYMENT_PROCESS_CHAIN, null, context);

    if (!response.isSuccess()) {
      throw propagateFailure(response);
    }

    // A replayed request never reached the persist node.
    return context.isReplay()
        ? context.getReplayedResponse()
        : context.getPaymentResponse();
  }

  private RuntimeException propagateFailure(LiteflowResponse response) {
    Throwable cause = response.getCause();
    if (cause instanceof RuntimeException runtimeException) {
      // Preserve the original domain exception (400/404/502/503 mapping).
      return runtimeException;
    }
    // A checked throwable is unexpected; surface as a generic 500 rather than
    // mislabelling it as a bank outage.
    LOG.error("Payment processing chain failed", cause);
    return new IllegalStateException("Payment processing failed", cause);
  }
}
