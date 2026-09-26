package com.checkout.payment.gateway.flow.node;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.IdempotencyConflictException;
import com.checkout.payment.gateway.flow.PaymentProcessContext;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.yomahub.liteflow.core.NodeComponent;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Chain node 2 (only effective when a key is supplied):
 * <ul>
 *   <li>key unknown -> proceed (the initiate node will claim it);</li>
 *   <li>key on a completed payment with the same attributes -> replay and
 *       end the chain, so the bank is never called twice;</li>
 *   <li>key on an INIT payment -> 409, the first attempt may still be
 *       reaching the bank, a retry now could double-charge;</li>
 *   <li>key on a completed payment with different attributes -> 409.</li>
 * </ul>
 */
@Component("checkIdempotency")
public class IdempotencyCheckNode extends NodeComponent {

  private final PaymentsRepository paymentsRepository;

  public IdempotencyCheckNode(PaymentsRepository paymentsRepository) {
    this.paymentsRepository = paymentsRepository;
  }

  @Override
  public void process() {
    PaymentProcessContext context = getContextBean(PaymentProcessContext.class);
    if (!context.hasIdempotencyKey()) {
      return;
    }

    Optional<PostPaymentResponse> existing =
        paymentsRepository.findByIdempotencyKey(context.getIdempotencyKey());
    if (existing.isEmpty()) {
      return;
    }

    PostPaymentResponse stored = existing.get();
    if (stored.getStatus() == PaymentStatus.INIT) {
      throw new IdempotencyConflictException(
          "A payment with this Idempotency-Key is still being initiated; retry later");
    }
    if (!describesSamePayment(context.getPaymentRequest(), stored)) {
      throw new IdempotencyConflictException(
          "Idempotency-Key was already used for a different payment request");
    }

    context.setReplayedResponse(stored);
    // Skip every remaining node: no bank call, no second persistence.
    setIsEnd(true);
  }

  /**
   * Compares only the non-sensitive payment attributes that are persisted;
   * full PAN/CVV are deliberately never part of this comparison.
   */
  private boolean describesSamePayment(PostPaymentRequest request,
      PostPaymentResponse stored) {
    String cardNumber = request.getCardNumber();
    int lastFour = Integer.parseInt(cardNumber.substring(cardNumber.length() - 4));
    return lastFour == stored.getCardNumberLastFour()
        && request.getExpiryMonth() == stored.getExpiryMonth()
        && request.getExpiryYear() == stored.getExpiryYear()
        && request.getAmount() == stored.getAmount()
        && request.getCurrency().equals(stored.getCurrency());
  }
}
