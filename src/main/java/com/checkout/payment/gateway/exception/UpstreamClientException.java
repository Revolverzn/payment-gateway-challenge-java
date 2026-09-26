package com.checkout.payment.gateway.exception;

/**
 * The acquiring bank rejected the gateway's request (HTTP 4xx).
 * This indicates an upstream contract/protocol problem rather than
 * the bank being unavailable, hence mapped to 502 Bad Gateway.
 */
public class UpstreamClientException extends RuntimeException {

  public UpstreamClientException(String message) {
    super(message);
  }
}
