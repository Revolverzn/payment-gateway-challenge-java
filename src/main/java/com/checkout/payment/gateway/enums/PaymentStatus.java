package com.checkout.payment.gateway.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PaymentStatus {
  // Local write-ahead state: our record exists but the bank has not answered
  // yet (call in flight, timed out or crashed). The result is unknown to us.
  INIT("Init"),
  // Upstream state: the bank explicitly reports the payment is still being
  // processed (e.g. asynchronous/stepped authentication). Not produced by
  // the current simulator, which only answers Authorized/Declined/503.
  PENDING("Pending"),
  AUTHORIZED("Authorized"),
  DECLINED("Declined"),
  REJECTED("Rejected");

  private final String name;

  PaymentStatus(String name) {
    this.name = name;
  }

  @JsonValue
  public String getName() {
    return this.name;
  }

  /**
   * Single place that translates the bank's authorization flag into a
   * gateway payment status.
   */
  public static PaymentStatus fromAuthorized(boolean authorized) {
    return authorized ? AUTHORIZED : DECLINED;
  }
}
