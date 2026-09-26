package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.exception.BankServiceException;
import com.checkout.payment.gateway.exception.UpstreamClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class AcquiringBankClient {

  private static final Logger LOG = LoggerFactory.getLogger(AcquiringBankClient.class);

  private final RestTemplate restTemplate;
  private final String bankUrl;

  public AcquiringBankClient(RestTemplate restTemplate,
      @Value("${bank.simulator.url}") String bankUrl) {
    this.restTemplate = restTemplate;
    this.bankUrl = bankUrl;
  }

  public BankResponse invoke(BankPaymentRequest request) {
    try {
      ResponseEntity<BankResponse> response = restTemplate.postForEntity(
          bankUrl, request, BankResponse.class);
      BankResponse body = response.getBody();
      if (body == null) {
        throw new BankServiceException("Empty response from bank");
      }
      return body;
    } catch (HttpClientErrorException e) {
      // 4xx: the bank rejected our request - an upstream contract problem.
      LOG.error("Bank rejected request with status {}: {}", e.getStatusCode(),
          e.getResponseBodyAsString(), e);
      throw new UpstreamClientException("Bank rejected the payment request");
    } catch (RestClientException e) {
      // 5xx and transport failures: the bank is unavailable.
      LOG.error("Error calling bank simulator", e);
      throw new BankServiceException("Bank service unavailable");
    }
  }
}
