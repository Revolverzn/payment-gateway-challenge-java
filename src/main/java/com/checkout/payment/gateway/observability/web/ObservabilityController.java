package com.checkout.payment.gateway.observability.web;

import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only feed behind the observability dashboard.
 *
 * <p>It joins two framework-provided sources per request, without collecting
 * anything itself:
 * <ul>
 *   <li>HTTP facts (time, method, path, status, latency) from the Actuator
 *       exchange journal, which has no bodies by design;</li>
 *   <li>business facts (payment id, status, amount, currency) from the
 *       existing payment store, located by a correlation key: the
 *       {@code X-Payment-Id} response header on POST (the journal records
 *       response headers), or the path id on GET {@code /payment/{id}}.</li>
 * </ul>
 * Idempotent replays carry the same header and therefore join to the same
 * stored payment. The model exposes no full PAN or CVV fields.
 */
@RestController
@RequestMapping("/observability")
@Tag(name = "Observability", description = "Read-only views for the dashboard")
public class ObservabilityController {

  static final int RECENT_ACTIVITY_LIMIT = 100;
  static final String PAYMENT_ID_HEADER = "X-Payment-Id";
  static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

  private final HttpExchangeRepository exchangeRepository;
  private final PaymentsRepository paymentsRepository;

  public ObservabilityController(HttpExchangeRepository exchangeRepository,
      PaymentsRepository paymentsRepository) {
    this.exchangeRepository = exchangeRepository;
    this.paymentsRepository = paymentsRepository;
  }

  @Operation(summary = "Recent HTTP activity joined with the stored payment per row")
  @GetMapping("/api/activity")
  public List<ActivityRow> recentActivity() {
    // Only the payment API belongs in this view; unrelated probes (favicon,
    // injected dev scripts, unknown paths) are excluded so they cannot skew
    // the dashboard's success-rate card. The journal returns newest first;
    // preserve that order and bound the feed.
    return exchangeRepository.findAll().stream()
        .filter(exchange -> isPaymentApiPath(exchange.getRequest().getUri().getPath()))
        .limit(RECENT_ACTIVITY_LIMIT)
        .map(this::toRow)
        .toList();
  }

  private static boolean isPaymentApiPath(String path) {
    return "/payments".equals(path) || path.startsWith("/payment/");
  }

  private ActivityRow toRow(HttpExchange exchange) {
    String path = exchange.getRequest().getUri().getPath();
    long durationMs = exchange.getTimeTaken() == null
        ? 0L : exchange.getTimeTaken().toMillis();

    UUID paymentId = resolvePaymentId(exchange, path);
    PostPaymentResponse payment = paymentId == null ? null
        : paymentsRepository.get(paymentId).orElse(null);

    return new ActivityRow(
        exchange.getTimestamp(),
        exchange.getRequest().getMethod(),
        path,
        exchange.getResponse().getStatus(),
        durationMs,
        firstHeader(exchange.getRequest().getHeaders(), IDEMPOTENCY_KEY_HEADER),
        paymentId == null ? null : paymentId.toString(),
        payment == null ? null : payment.getStatus().getName(),
        payment == null ? null : payment.getAmount(),
        payment == null ? null : payment.getCurrency());
  }

  private static String firstHeader(Map<String, List<String>> headers, String name) {
    for (Map.Entry<String, List<String>> header : headers.entrySet()) {
      if (name.equalsIgnoreCase(header.getKey()) && !header.getValue().isEmpty()) {
        return header.getValue().get(0);
      }
    }
    return null;
  }

  private UUID resolvePaymentId(HttpExchange exchange, String path) {
    if ("GET".equals(exchange.getRequest().getMethod())
        && path.startsWith("/payment/")) {
      return parseUuid(path.substring(path.lastIndexOf('/') + 1));
    }
    if (exchange.getResponse().getStatus() >= 200
        && exchange.getResponse().getStatus() < 300) {
      for (Map.Entry<String, List<String>> header :
          exchange.getResponse().getHeaders().entrySet()) {
        if (PAYMENT_ID_HEADER.equalsIgnoreCase(header.getKey())
            && !header.getValue().isEmpty()) {
          return parseUuid(header.getValue().get(0));
        }
      }
    }
    return null;
  }

  private UUID parseUuid(String value) {
    try {
      return UUID.fromString(value.toLowerCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
