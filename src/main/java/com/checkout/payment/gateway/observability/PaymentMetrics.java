package com.checkout.payment.gateway.observability;

import com.checkout.payment.gateway.enums.PaymentStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Business metrics for payment processing, exposed via the actuator
 * {@code metrics}/{@code prometheus} endpoints:
 * <ul>
 *   <li>{@code payments_processed_total{status=Authorized|Declined}} —
 *       payments that reached the bank and were persisted</li>
 *   <li>{@code payments_replayed_total} — retries served from the idempotency
 *       record without calling the bank</li>
 * </ul>
 */
@Component
public class PaymentMetrics {

  private final MeterRegistry meterRegistry;

  public PaymentMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void recordProcessed(PaymentStatus status) {
    Counter.builder("payments.processed")
        .description("Payments processed and persisted, tagged by bank outcome")
        .tag("status", status.getName())
        .register(meterRegistry)
        .increment();
  }

  public void recordReplay() {
    Counter.builder("payments.replayed")
        .description("Payment requests replayed from an idempotency record")
        .register(meterRegistry)
        .increment();
  }
}
