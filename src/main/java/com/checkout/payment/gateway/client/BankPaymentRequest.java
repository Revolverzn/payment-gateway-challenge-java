package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request payload contract of the acquiring bank simulator.
 * Keeping this DTO separate from {@link PostPaymentRequest} decouples the
 * bank's external contract from the gateway's inbound API contract.
 */
public class BankPaymentRequest {

  @JsonProperty("card_number")
  private String cardNumber;
  @JsonProperty("expiry_date")
  private String expiryDate;
  private String currency;
  private int amount;
  private String cvv;

  public BankPaymentRequest() {
  }

  public BankPaymentRequest(String cardNumber, String expiryDate, String currency,
      int amount, String cvv) {
    this.cardNumber = cardNumber;
    this.expiryDate = expiryDate;
    this.currency = currency;
    this.amount = amount;
    this.cvv = cvv;
  }

  public static BankPaymentRequest from(PostPaymentRequest request) {
    // The bank's MM/yyyy wire format is this DTO's concern, not the inbound
    // gateway model's. The month must be zero-padded (e.g. 4 -> "04/2030").
    String expiryDate = String.format("%02d/%d",
        request.getExpiryMonth(), request.getExpiryYear());
    return new BankPaymentRequest(
        request.getCardNumber(),
        expiryDate,
        request.getCurrency(),
        request.getAmount(),
        String.valueOf(request.getCvv()));
  }

  public String getCardNumber() {
    return cardNumber;
  }

  public String getExpiryDate() {
    return expiryDate;
  }

  public String getCurrency() {
    return currency;
  }

  public int getAmount() {
    return amount;
  }

  public String getCvv() {
    return cvv;
  }

  @Override
  public String toString() {
    return "BankPaymentRequest{" +
        "cardNumber='" + maskedCardNumber() + '\'' +
        ", expiryDate='" + expiryDate + '\'' +
        ", currency='" + currency + '\'' +
        ", amount=" + amount +
        ", cvv='***'" +
        '}';
  }

  private String maskedCardNumber() {
    if (cardNumber == null || cardNumber.length() < 4) {
      return "****";
    }
    return "****" + cardNumber.substring(cardNumber.length() - 4);
  }
}
