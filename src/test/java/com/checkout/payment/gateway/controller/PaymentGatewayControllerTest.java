package com.checkout.payment.gateway.controller;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.client.BankResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  PaymentsRepository paymentsRepository;
  @MockBean
  private RestTemplate restTemplate;

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    PostPaymentResponse payment = new PostPaymentResponse();
    payment.setId(UUID.randomUUID());
    payment.setAmount(10);
    payment.setCurrency("USD");
    payment.setStatus(PaymentStatus.AUTHORIZED);
    payment.setExpiryMonth(12);
    payment.setExpiryYear(2024);
    payment.setCardNumberLastFour(4321);

    paymentsRepository.add(payment);

    mvc.perform(MockMvcRequestBuilders.get("/payment/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(payment.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiryMonth").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    UUID missingId = UUID.randomUUID();
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + missingId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Payment not found with id " + missingId));
  }

  @Test
  void whenValidPaymentWithCardEndingOddThenAuthorized() throws Exception {
    BankResponse bankResponse = new BankResponse();
    bankResponse.setAuthorized(true);
    bankResponse.setAuthorizationCode("test-auth-code");
    when(restTemplate.postForEntity(anyString(), any(), eq(BankResponse.class)))
        .thenReturn(new ResponseEntity<>(bankResponse, HttpStatus.OK));

    String requestBody = "{\"card_number\":\"2222405343248877\","
        + "\"expiry_month\":12,\"expiry_year\":2030,"
        + "\"currency\":\"GBP\",\"amount\":100,\"cvv\":123}";

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.cardNumberLastFour").value(8877))
        .andExpect(jsonPath("$.expiryMonth").value(12))
        .andExpect(jsonPath("$.expiryYear").value(2030))
        .andExpect(jsonPath("$.currency").value("GBP"))
        .andExpect(jsonPath("$.amount").value(100));
  }

  @Test
  void whenValidPaymentWithCardEndingEvenThenDeclined() throws Exception {
    BankResponse bankResponse = new BankResponse();
    bankResponse.setAuthorized(false);
    bankResponse.setAuthorizationCode("");
    when(restTemplate.postForEntity(anyString(), any(), eq(BankResponse.class)))
        .thenReturn(new ResponseEntity<>(bankResponse, HttpStatus.OK));

    String requestBody = "{\"card_number\":\"2222405343248878\","
        + "\"expiry_month\":12,\"expiry_year\":2030,"
        + "\"currency\":\"GBP\",\"amount\":100,\"cvv\":123}";

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("Declined"))
        .andExpect(jsonPath("$.cardNumberLastFour").value(8878));
  }

  @Test
  void whenInvalidPaymentThen400WithStructuredFieldErrorsIsReturned() throws Exception {
    String requestBody = "{\"card_number\":\"123\","
        + "\"expiry_month\":13,\"expiry_year\":2020,"
        + "\"currency\":\"XXX\",\"amount\":-100,\"cvv\":12}";

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors").isNotEmpty())
        .andExpect(jsonPath("$.errors[0].field").value("card_number"))
        .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
  }

  @Test
  void whenBankReturns503Then503IsReturned() throws Exception {
    when(restTemplate.postForEntity(anyString(), any(), eq(BankResponse.class)))
        .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

    String requestBody = "{\"card_number\":\"2222405343248880\","
        + "\"expiry_month\":12,\"expiry_year\":2030,"
        + "\"currency\":\"GBP\",\"amount\":100,\"cvv\":123}";

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message").value("Bank service unavailable"));
  }

  @Test
  void whenCardNumberTooShortThen400IsReturned() throws Exception {
    String requestBody = "{\"card_number\":\"1234567890123\","
        + "\"expiry_month\":12,\"expiry_year\":2030,"
        + "\"currency\":\"GBP\",\"amount\":100,\"cvv\":123}";

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("card_number"));
  }

  @Test
  void whenCardNumberNotNumericThen400IsReturned() throws Exception {
    String requestBody = "{\"card_number\":\"abcd405343248877\","
        + "\"expiry_month\":12,\"expiry_year\":2030,"
        + "\"currency\":\"GBP\",\"amount\":100,\"cvv\":123}";

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("card_number"));
  }

  @Test
  void whenCurrencyNotSupportedThen400IsReturned() throws Exception {
    String requestBody = "{\"card_number\":\"2222405343248877\","
        + "\"expiry_month\":12,\"expiry_year\":2030,"
        + "\"currency\":\"JPY\",\"amount\":100,\"cvv\":123}";

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("currency"));
  }

  @Test
  void whenCvvTooLongThen400IsReturned() throws Exception {
    String requestBody = "{\"card_number\":\"2222405343248877\","
        + "\"expiry_month\":12,\"expiry_year\":2030,"
        + "\"currency\":\"GBP\",\"amount\":100,\"cvv\":12345}";

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("cvv"));
  }

  @Test
  void whenPaymentRejectedThenBankIsNeverCalled() throws Exception {
    String requestBody = "{\"card_number\":\"123\","
        + "\"expiry_month\":13,\"expiry_year\":2020,"
        + "\"currency\":\"XXX\",\"amount\":-100,\"cvv\":12}";

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(restTemplate);
  }

  @Test
  void whenPaymentProcessedThenItCanBeRetrievedByIdWithMaskedCard() throws Exception {
    BankResponse bankResponse = new BankResponse();
    bankResponse.setAuthorized(true);
    bankResponse.setAuthorizationCode("test-auth-code");
    when(restTemplate.postForEntity(anyString(), any(), eq(BankResponse.class)))
        .thenReturn(new ResponseEntity<>(bankResponse, HttpStatus.OK));

    String requestBody = "{\"card_number\":\"2222405343248877\","
        + "\"expiry_month\":4,\"expiry_year\":2030,"
        + "\"currency\":\"GBP\",\"amount\":100,\"cvv\":123}";

    String postResponseBody = mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    String paymentId = com.jayway.jsonpath.JsonPath.read(postResponseBody, "$.id");

    mvc.perform(MockMvcRequestBuilders.get("/payment/" + paymentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(paymentId))
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.cardNumberLastFour").value(8877))
        .andExpect(jsonPath("$.expiryMonth").value(4))
        .andExpect(jsonPath("$.expiryYear").value(2030))
        .andExpect(jsonPath("$.currency").value("GBP"))
        .andExpect(jsonPath("$.amount").value(100));
  }

  @Test
  void whenRequestBodyMalformedThen400WithErrorResponseIsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{not-valid-json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed request body"));
  }

  @Test
  void whenPaymentIdIsNotUuidThen400WithErrorResponseIsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/payment/not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").exists());
  }

  @Test
  void whenSameIdempotencyKeySentTwiceThenBankIsCalledOnceAndSamePaymentReturned()
      throws Exception {
    BankResponse bankResponse = new BankResponse();
    bankResponse.setAuthorized(true);
    bankResponse.setAuthorizationCode("test-auth-code");
    when(restTemplate.postForEntity(anyString(), any(), eq(BankResponse.class)))
        .thenReturn(new ResponseEntity<>(bankResponse, HttpStatus.OK));

    String requestBody = "{\"card_number\":\"2222405343248877\","
        + "\"expiry_month\":12,\"expiry_year\":2030,"
        + "\"currency\":\"GBP\",\"amount\":100,\"cvv\":123}";
    String key = UUID.randomUUID().toString();

    String firstBody = mvc.perform(MockMvcRequestBuilders.post("/payments")
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    String secondBody = mvc.perform(MockMvcRequestBuilders.post("/payments")
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andReturn().getResponse().getContentAsString();

    String firstId = com.jayway.jsonpath.JsonPath.read(firstBody, "$.id");
    String secondId = com.jayway.jsonpath.JsonPath.read(secondBody, "$.id");
    org.junit.jupiter.api.Assertions.assertEquals(firstId, secondId);
    verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(BankResponse.class));
  }

  @Test
  void whenRejectedRequestHasIdempotencyKeyThenKeyIsNotStoredAndBankNotCalled()
      throws Exception {
    String key = UUID.randomUUID().toString();
    String invalidBody = "{\"card_number\":\"123\","
        + "\"expiry_month\":13,\"expiry_year\":2020,"
        + "\"currency\":\"XXX\",\"amount\":-100,\"cvv\":12}";

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidBody))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(restTemplate);
  }

  @Test
  void whenBankReturns4xxThen502IsReturned() throws Exception {
    when(restTemplate.postForEntity(anyString(), any(), eq(BankResponse.class)))
        .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

    String requestBody = "{\"card_number\":\"2222405343248877\","
        + "\"expiry_month\":12,\"expiry_year\":2030,"
        + "\"currency\":\"GBP\",\"amount\":100,\"cvv\":123}";

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Bank rejected the payment request"));
  }

  @Test
  void whenSameIdempotencyKeyReusedWithDifferentPayloadThen409AndBankCalledOnce()
      throws Exception {
    BankResponse bankResponse = new BankResponse();
    bankResponse.setAuthorized(true);
    bankResponse.setAuthorizationCode("test-auth-code");
    when(restTemplate.postForEntity(anyString(), any(), eq(BankResponse.class)))
        .thenReturn(new ResponseEntity<>(bankResponse, HttpStatus.OK));

    String firstBody = "{\"card_number\":\"2222405343248877\","
        + "\"expiry_month\":12,\"expiry_year\":2030,"
        + "\"currency\":\"GBP\",\"amount\":100,\"cvv\":123}";
    String differentBody = "{\"card_number\":\"2222405343248877\","
        + "\"expiry_month\":12,\"expiry_year\":2030,"
        + "\"currency\":\"GBP\",\"amount\":999,\"cvv\":123}";
    String key = UUID.randomUUID().toString();

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(firstBody))
        .andExpect(status().isCreated());

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(differentBody))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").exists());

    verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(BankResponse.class));
  }

  @Test
  void whenBankTimesOutThenInitRecordRemainsForReconciliation() throws Exception {
    when(restTemplate.postForEntity(anyString(), any(), eq(BankResponse.class)))
        .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

    String key = UUID.randomUUID().toString();
    String requestBody = "{\"card_number\":\"2222405343248880\","
        + "\"expiry_month\":12,\"expiry_year\":2030,"
        + "\"currency\":\"GBP\",\"amount\":100,\"cvv\":123}";

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isServiceUnavailable());

    // Write-ahead: the attempt did not vanish — it is stored in the local
    // INIT state so a reconciliation job can discover the in-doubt payment.
    PostPaymentResponse stored = paymentsRepository.findByIdempotencyKey(key).orElseThrow();
    org.junit.jupiter.api.Assertions.assertEquals(PaymentStatus.INIT, stored.getStatus());
  }

  @Test
  void whenSameKeyArrivesWhilePaymentIsInitThen409AndBankNeverCalled() throws Exception {
    String key = UUID.randomUUID().toString();
    PostPaymentResponse initiated = new PostPaymentResponse();
    initiated.setId(UUID.randomUUID());
    initiated.setStatus(PaymentStatus.INIT);
    initiated.setCardNumberLastFour(8877);
    initiated.setExpiryMonth(12);
    initiated.setExpiryYear(2030);
    initiated.setCurrency("GBP");
    initiated.setAmount(100);
    initiated.setIdempotencyKey(key);
    paymentsRepository.add(initiated);

    String requestBody = "{\"card_number\":\"2222405343248877\","
        + "\"expiry_month\":12,\"expiry_year\":2030,"
        + "\"currency\":\"GBP\",\"amount\":100,\"cvv\":123}";

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isConflict());

    verifyNoInteractions(restTemplate);
  }

  @Test
  void whenInitPaymentRetrievedThenItsLocalInitStatusIsVisible() throws Exception {
    PostPaymentResponse initiated = new PostPaymentResponse();
    initiated.setId(UUID.randomUUID());
    initiated.setStatus(PaymentStatus.INIT);
    initiated.setCardNumberLastFour(8877);
    initiated.setExpiryMonth(12);
    initiated.setExpiryYear(2030);
    initiated.setCurrency("GBP");
    initiated.setAmount(100);
    paymentsRepository.add(initiated);

    mvc.perform(MockMvcRequestBuilders.get("/payment/" + initiated.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Init"));
  }

  @Test
  void whenPaymentAuthorizedThenBankAuthorizationCodeIsPersistedAndReturned() throws Exception {
    BankResponse bankResponse = new BankResponse();
    bankResponse.setAuthorized(true);
    bankResponse.setAuthorizationCode("auth-code-123");
    when(restTemplate.postForEntity(anyString(), any(), eq(BankResponse.class)))
        .thenReturn(new ResponseEntity<>(bankResponse, HttpStatus.OK));

    String requestBody = "{\"card_number\":\"2222405343248877\","
        + "\"expiry_month\":12,\"expiry_year\":2030,"
        + "\"currency\":\"GBP\",\"amount\":100,\"cvv\":123}";

    String responseBody = mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.authorizationCode").value("auth-code-123"))
        .andReturn().getResponse().getContentAsString();

    String paymentId = new com.fasterxml.jackson.databind.ObjectMapper()
        .readTree(responseBody).get("id").asText();

    // The acquirer reference is persisted: a later GET returns it too.
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + paymentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.authorizationCode").value("auth-code-123"));

    org.junit.jupiter.api.Assertions.assertEquals("auth-code-123",
        paymentsRepository.get(UUID.fromString(paymentId)).orElseThrow()
            .getAuthorizationCode());
  }

  @Test
  void whenPaymentDeclinedThenEmptyBankAuthorizationCodeIsOmitted() throws Exception {
    BankResponse bankResponse = new BankResponse();
    bankResponse.setAuthorized(false);
    bankResponse.setAuthorizationCode("");
    when(restTemplate.postForEntity(anyString(), any(), eq(BankResponse.class)))
        .thenReturn(new ResponseEntity<>(bankResponse, HttpStatus.OK));

    String requestBody = "{\"card_number\":\"2222405343248878\","
        + "\"expiry_month\":12,\"expiry_year\":2030,"
        + "\"currency\":\"GBP\",\"amount\":100,\"cvv\":123}";

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("Declined"))
        .andExpect(jsonPath("$.authorizationCode").doesNotExist());
  }
}
