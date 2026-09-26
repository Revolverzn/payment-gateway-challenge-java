package com.checkout.payment.gateway.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.checkout.payment.gateway.exception.BankServiceException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class AcquiringBankClientTest {

  private static final String BANK_URL = "http://localhost:8080/payments";

  private RestTemplate restTemplate;
  private MockRestServiceServer server;
  private AcquiringBankClient bankClient;

  @BeforeEach
  void setUp() {
    restTemplate = new RestTemplate();
    server = MockRestServiceServer.bindTo(restTemplate).build();
    bankClient = new AcquiringBankClient(restTemplate, BANK_URL);
  }

  @Test
  void invokeSendsBankContractPayloadAndParsesAuthorizedResponse() {
    server.expect(requestTo(BANK_URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("""
            {
              "card_number": "2222405343248877",
              "expiry_date": "04/2030",
              "currency": "GBP",
              "amount": 100,
              "cvv": "123"
            }
            """))
        .andRespond(withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {"authorized": true, "authorization_code": "0bb07405-6d44"}
                """));

    BankResponse response = bankClient.invoke(BankPaymentRequest.from(validRequest()));

    assertTrue(response.isAuthorized());
    assertEquals("0bb07405-6d44", response.getAuthorizationCode());
    server.verify();
  }

  @Test
  void invokeParsesDeclinedResponse() {
    server.expect(requestTo(BANK_URL))
        .andRespond(withStatus(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"authorized\": false, \"authorization_code\": \"\"}"));

    BankResponse response = bankClient.invoke(BankPaymentRequest.from(validRequest()));

    assertFalse(response.isAuthorized());
    server.verify();
  }

  @Test
  void invokeWhenBankReturns503ThrowsBankServiceException() {
    server.expect(requestTo(BANK_URL))
        .andRespond(withServerError());

    assertThrows(BankServiceException.class,
        () -> bankClient.invoke(BankPaymentRequest.from(validRequest())));
    server.verify();
  }

  @Test
  void invokeWhenBankReturnsEmptyBodyThrowsBankServiceException() {
    server.expect(requestTo(BANK_URL))
        .andRespond(withStatus(HttpStatus.OK));

    assertThrows(BankServiceException.class,
        () -> bankClient.invoke(BankPaymentRequest.from(validRequest())));
    server.verify();
  }

  @Test
  void invokeWhenBankReturns4xxThrowsUpstreamClientException() {
    server.expect(requestTo(BANK_URL))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST));

    assertThrows(com.checkout.payment.gateway.exception.UpstreamClientException.class,
        () -> bankClient.invoke(BankPaymentRequest.from(validRequest())));
    server.verify();
  }

  @Test
  void invokeWhenBankResponseExceedsReadTimeoutThrowsBankServiceException() throws Exception {
    // A real HTTP server is required here: MockRestServiceServer mocks below the
    // socket layer, so a socket read timeout can never be exercised with it.
    HttpServer slowServer = HttpServer.create(new InetSocketAddress(0), 0);
    slowServer.createContext("/payments", exchange -> {
      try {
        Thread.sleep(1000);
        byte[] body = "{\"authorized\":true,\"authorization_code\":\"x\"}"
            .getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
          os.write(body);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });
    slowServer.start();
    try {
      String slowUrl = "http://localhost:" + slowServer.getAddress().getPort() + "/payments";

      SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
      factory.setReadTimeout(50);
      AcquiringBankClient slowBankClient =
          new AcquiringBankClient(new RestTemplate(factory), slowUrl);

      assertThrows(BankServiceException.class,
          () -> slowBankClient.invoke(BankPaymentRequest.from(validRequest())));
    } finally {
      slowServer.stop(0);
    }
  }

  private PostPaymentRequest validRequest() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("2222405343248877");
    request.setExpiryMonth(4);
    request.setExpiryYear(2030);
    request.setCurrency("GBP");
    request.setAmount(100);
    request.setCvv(123);
    return request;
  }
}
