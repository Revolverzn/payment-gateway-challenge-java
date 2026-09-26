package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

public class PostPaymentRequest implements Serializable {

  @JsonProperty("card_number")
  @Schema(description = "Full PAN; 14-19 digits. Never stored or returned.",
      example = "2222405343248877", requiredMode = Schema.RequiredMode.REQUIRED)
  private String cardNumber;
  @JsonProperty("card_number_last_four")
  @Schema(hidden = true)
  private int cardNumberLastFour;
  @JsonProperty("expiry_month")
  @Schema(example = "12", minimum = "1", maximum = "12",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private int expiryMonth;
  @JsonProperty("expiry_year")
  @Schema(description = "Combined month/year must be in the future",
      example = "2030", requiredMode = Schema.RequiredMode.REQUIRED)
  private int expiryYear;
  @Schema(example = "GBP", allowableValues = {"USD", "GBP", "EUR"},
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String currency;
  @Schema(description = "Amount in minor currency units, e.g. 1050 = GBP 10.50",
      example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
  private int amount;
  @Schema(example = "123", minimum = "100", maximum = "9999",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private int cvv;

  public String getCardNumber() {
    return cardNumber;
  }

  public void setCardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
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

  public int getCvv() {
    return cvv;
  }

  public void setCvv(int cvv) {
    this.cvv = cvv;
  }

  @Override
  public String toString() {
    return "PostPaymentRequest{" +
        "cardNumber='" + maskCardNumber() + '\'' +
        ", cardNumberLastFour=" + cardNumberLastFour +
        ", expiryMonth=" + expiryMonth +
        ", expiryYear=" + expiryYear +
        ", currency='" + currency + '\'' +
        ", amount=" + amount +
        ", cvv='***'" +
        '}';
  }

  private String maskCardNumber() {
    if (cardNumber == null || cardNumber.length() < 4) {
      return "****";
    }
    return "****" + cardNumber.substring(cardNumber.length() - 4);
  }
}
