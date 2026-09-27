package com.checkout.payment.gateway.observability.web;

import java.time.Instant;

/**
 * One row of the dashboard's unified activity view: the HTTP facts from the
 * exchange journal joined with the business facts of the stored payment.
 *
 * <p>Payment fields are {@code null} for requests that produced no payment
 * (validation rejection, bank outage, a 404 lookup) — the row still shows
 * what happened at the HTTP level. The idempotency key comes from the
 * request header, so it is present even when the request was rejected.
 */
public record ActivityRow(
    Instant timestamp,
    String method,
    String path,
    int httpStatus,
    long durationMs,
    String idempotencyKey,
    String paymentId,
    String paymentStatus,
    Integer amount,
    String currency) {
}
