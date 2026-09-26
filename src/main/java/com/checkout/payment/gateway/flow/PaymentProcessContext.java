package com.checkout.payment.gateway.flow;

import com.checkout.payment.gateway.client.BankPaymentRequest;
import com.checkout.payment.gateway.client.BankResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;

/**
 * Request-scoped data carrier for the payment processing chain.
 * A new instance is created for every request, so the singleton
 * LiteFlow nodes stay stateless while sharing data through this context.
 */
public class PaymentProcessContext {

  private final PostPaymentRequest paymentRequest;
  private final String idempotencyKey;
  private BankPaymentRequest bankRequest;
  private BankResponse bankResponse;
  // Parsed once from bankResponse by the parse node; later nodes read these
  // flat fields and never touch the raw bankResponse.
  private PaymentStatus paymentStatus;
  private String authorizationCode;
  private PostPaymentResponse paymentResponse;
  private PostPaymentResponse replayedResponse;

  public PaymentProcessContext(PostPaymentRequest paymentRequest, String idempotencyKey) {
    this.paymentRequest = paymentRequest;
    this.idempotencyKey = idempotencyKey;
  }

  public PostPaymentRequest getPaymentRequest() {
    return paymentRequest;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public boolean hasIdempotencyKey() {
    return idempotencyKey != null && !idempotencyKey.isBlank();
  }

  public BankPaymentRequest getBankRequest() {
    return bankRequest;
  }

  public void setBankRequest(BankPaymentRequest bankRequest) {
    this.bankRequest = bankRequest;
  }

  public BankResponse getBankResponse() {
    return bankResponse;
  }

  public void setBankResponse(BankResponse bankResponse) {
    this.bankResponse = bankResponse;
  }

  public PaymentStatus getPaymentStatus() {
    return paymentStatus;
  }

  public void setPaymentStatus(PaymentStatus paymentStatus) {
    this.paymentStatus = paymentStatus;
  }

  public String getAuthorizationCode() {
    return authorizationCode;
  }

  public void setAuthorizationCode(String authorizationCode) {
    this.authorizationCode = authorizationCode;
  }

  public PostPaymentResponse getPaymentResponse() {
    return paymentResponse;
  }

  public void setPaymentResponse(PostPaymentResponse paymentResponse) {
    this.paymentResponse = paymentResponse;
  }

  public PostPaymentResponse getReplayedResponse() {
    return replayedResponse;
  }

  public void setReplayedResponse(PostPaymentResponse replayedResponse) {
    this.replayedResponse = replayedResponse;
  }

  public boolean isReplay() {
    return replayedResponse != null;
  }
}
