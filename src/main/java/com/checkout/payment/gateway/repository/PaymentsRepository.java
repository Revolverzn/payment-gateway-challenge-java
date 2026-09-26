package com.checkout.payment.gateway.repository;

import com.checkout.payment.gateway.model.PostPaymentResponse;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Repository;

/**
 * In-memory payment store.
 *
 * <p>The idempotency key is part of the payment record, mirroring a real
 * {@code payments} table with a unique index on {@code idempotency_key}:
 * looking up by key replays a previous payment, and inserting a duplicate key
 * atomically returns the row that won the race.
 */
@Repository
public class PaymentsRepository {

  private final ConcurrentMap<UUID, PostPaymentResponse> payments = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, UUID> idempotencyIndex = new ConcurrentHashMap<>();

  /**
   * Stores the payment. When an idempotency key is present and another payment
   * already owns it, that earlier payment is returned and the given one is
   * not stored — the in-memory equivalent of a unique-constraint conflict.
   * The check-and-insert sequence is atomic within this single node.
   *
   * @return the payment stored for this identity (never {@code null})
   */
  public synchronized PostPaymentResponse add(PostPaymentResponse payment) {
    String key = payment.getIdempotencyKey();
    if (key != null && !key.isBlank()) {
      UUID existingId = idempotencyIndex.get(key);
      if (existingId != null) {
        return payments.get(existingId);
      }
    }

    payments.put(payment.getId(), payment);
    if (key != null && !key.isBlank()) {
      idempotencyIndex.put(key, payment.getId());
    }
    return payment;
  }

  /**
   * Writes back an existing payment after its status transitioned to a
   * terminal state (the in-memory equivalent of an UPDATE by id).
   */
  public PostPaymentResponse save(PostPaymentResponse payment) {
    payments.put(payment.getId(), payment);
    return payment;
  }

  public Optional<PostPaymentResponse> get(UUID id) {
    return Optional.ofNullable(payments.get(id));
  }

  public Optional<PostPaymentResponse> findByIdempotencyKey(String idempotencyKey) {
    return Optional.ofNullable(idempotencyIndex.get(idempotencyKey)).map(payments::get);
  }

}
