package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public class PostPaymentResponse {
  @Schema(example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
  private UUID id;
  private PaymentStatus status;
  @Schema(description = "Only the last four PAN digits are ever returned", example = "8877")
  private int cardNumberLastFour;
  private int expiryMonth;
  private int expiryYear;
  private String currency;
  private int amount;
  // Acquirer's reference for a successful authorization; absent for INIT and
  // Declined payments (the bank returns an empty code on a decline).
  @Schema(description = "Acquirer authorization code, present only when Authorized",
      example = "a3f5c2e1-8b9d-4e7a-9c1f-2d6e4b8a0f13")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String authorizationCode;
  // Internal dedup key; never serialized back to the merchant.
  @JsonIgnore
  private String idempotencyKey;


  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public void setStatus(PaymentStatus status) {
    this.status = status;
  }

  public int getCardNumberLastFour() {
    return cardNumberLastFour;
  }

  public void setCardNumberLastFour(int cardNumberLastFour) {
    this.cardNumberLastFour = cardNumberLastFour;
  }

  public int getExpiryMonth() {
    return expiryMonth;
  }

  public void setExpiryMonth(int expiryMonth) {
    this.expiryMonth = expiryMonth;
  }

  public int getExpiryYear() {
    return expiryYear;
  }

  public void setExpiryYear(int expiryYear) {
    this.expiryYear = expiryYear;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public int getAmount() {
    return amount;
  }

  public void setAmount(int amount) {
    this.amount = amount;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(String idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
  }

  public String getAuthorizationCode() {
    return authorizationCode;
  }

  public void setAuthorizationCode(String authorizationCode) {
    this.authorizationCode = authorizationCode;
  }

  public static PostPaymentResponse from(PostPaymentRequest request, PaymentStatus status,
      String idempotencyKey) {
    PostPaymentResponse response = new PostPaymentResponse();
    response.setId(UUID.randomUUID());
    response.setStatus(status);
    String cardNumber = request.getCardNumber();
    response.setCardNumberLastFour(
        Integer.parseInt(cardNumber.substring(cardNumber.length() - 4)));
    response.setExpiryMonth(request.getExpiryMonth());
    response.setExpiryYear(request.getExpiryYear());
    response.setCurrency(request.getCurrency());
    response.setAmount(request.getAmount());
    response.setIdempotencyKey(idempotencyKey);
    return response;
  }

  @Override
  public String toString() {
    return "GetPaymentResponse{" +
        "id=" + id +
        ", status=" + status +
        ", cardNumberLastFour=" + cardNumberLastFour +
        ", expiryMonth=" + expiryMonth +
        ", expiryYear=" + expiryYear +
        ", currency='" + currency + '\'' +
        ", amount=" + amount +
        ", authorizationCode='" + authorizationCode + '\'' +
        '}';
  }
}
