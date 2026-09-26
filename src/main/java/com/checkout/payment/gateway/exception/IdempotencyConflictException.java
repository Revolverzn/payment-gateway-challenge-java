package com.checkout.payment.gateway.exception;

/**
 * A request reused an {@code Idempotency-Key} that was already consumed by a
 * <em>different</em> payment. Replaying the first result would be wrong, so
 * the request is rejected with 409 Conflict instead of silently processing a
 * second payment.
 */
public class IdempotencyConflictException extends RuntimeException {

  public IdempotencyConflictException(String message) {
    super(message);
  }
}
