package com.checkout.payment.gateway.flow.node;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.IdempotencyConflictException;
import com.checkout.payment.gateway.flow.PaymentProcessContext;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.observability.PaymentMetrics;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.yomahub.liteflow.core.NodeComponent;
import org.springframework.stereotype.Component;

/**
 * Chain node 4 (write-ahead): creates the payment record in INIT and
 * persists it <em>before</em> the bank is called. INIT is our local state —
 * the record exists but the bank has not answered. If the call later times
 * out or the process crashes, the attempt still exists in INIT and can be
 * found and reconciled instead of vanishing without a trace.
 *
 * <p>The atomic insert also claims the idempotency key. When two concurrent
 * first requests race, exactly one insert wins; the loser must not call the
 * bank — it replays a completed winner or gets 409 against an INIT one.
 */
@Component("createPaymentOrder")
public class PaymentInitiateNode extends NodeComponent {

  private final PaymentsRepository paymentsRepository;
  private final PaymentMetrics paymentMetrics;

  public PaymentInitiateNode(PaymentsRepository paymentsRepository,
      PaymentMetrics paymentMetrics) {
    this.paymentsRepository = paymentsRepository;
    this.paymentMetrics = paymentMetrics;
  }

  @Override
  public void process() {
    PaymentProcessContext context = getContextBean(PaymentProcessContext.class);
    PostPaymentResponse initiated = PostPaymentResponse.from(
        context.getPaymentRequest(), PaymentStatus.INIT, context.getIdempotencyKey());

    PostPaymentResponse stored = paymentsRepository.add(initiated);
    if (stored != initiated) {
      // Lost the race for the idempotency key to a concurrent request.
      if (stored.getStatus() == PaymentStatus.INIT) {
        throw new IdempotencyConflictException(
            "A payment with this Idempotency-Key is still being initiated; retry later");
      }
      context.setReplayedResponse(stored);
      paymentMetrics.recordReplay();
      setIsEnd(true);
      return;
    }
    context.setPaymentResponse(stored);
  }
}
