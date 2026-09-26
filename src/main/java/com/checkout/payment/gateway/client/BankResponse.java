package com.checkout.payment.gateway.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BankResponse {

  @JsonProperty("authorized")
  private boolean authorized;

  @JsonProperty("authorization_code")
  private String authorizationCode;

  public boolean isAuthorized() {
    return authorized;
  }

  public void setAuthorized(boolean authorized) {
    this.authorized = authorized;
  }

  public String getAuthorizationCode() {
    return authorizationCode;
  }

  public void setAuthorizationCode(String authorizationCode) {
    this.authorizationCode = authorizationCode;
  }

  @Override
  public String toString() {
    return "BankResponse{" +
        "authorized=" + authorized +
        ", authorizationCode='" + authorizationCode + '\'' +
        '}';
  }
}
