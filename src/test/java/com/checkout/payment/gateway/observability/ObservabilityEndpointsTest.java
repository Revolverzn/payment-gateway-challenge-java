package com.checkout.payment.gateway.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.client.BankResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;

/**
 * Verifies the framework-provided observability surface: the HTTP exchange
 * journal ({@code httpexchanges}) and Micrometer business metrics.
 *
 * <p>Spring Boot's test support deliberately swaps real metrics exporters
 * (e.g. Prometheus) for an in-memory {@link MeterRegistry}, so the custom
 * counter is asserted directly on the registry rather than by scraping the
 * Prometheus endpoint; that endpoint is framework-wired and verified
 * manually when the app runs.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ObservabilityEndpointsTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  private MeterRegistry meterRegistry;
  @MockBean
  private RestTemplate restTemplate;

  @Test
  void humanReadableDashboardPageIsServed() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/observability.html"))
        .andExpect(status().isOk());
  }

  @Test
  void httpexchangesJournalKeepsBusinessTrafficAndIgnoresItsOwnPolling() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + UUID.randomUUID()))
        .andExpect(status().isNotFound());
    // Dashboard/monitoring traffic must not pollute (and evict) the journal.
    mvc.perform(MockMvcRequestBuilders.get("/actuator/health"))
        .andExpect(status().isOk());
    mvc.perform(MockMvcRequestBuilders.get("/observability/api/activity"))
        .andExpect(status().isOk());
    mvc.perform(MockMvcRequestBuilders.get("/actuator/httpexchanges"))
        .andExpect(status().isOk());

    mvc.perform(MockMvcRequestBuilders.get("/actuator/httpexchanges"))
        .andExpect(status().isOk())
        // The most recent recorded exchange is the 404 business lookup; it
        // carries the request line, response status and a timestamp.
        .andExpect(jsonPath("$.exchanges[0].request.method").value("GET"))
        .andExpect(jsonPath("$.exchanges[0].request.uri").isNotEmpty())
        .andExpect(jsonPath("$.exchanges[0].response.status").value(404))
        .andExpect(jsonPath("$.exchanges[0].timestamp").isNotEmpty())
        .andExpect(jsonPath("$.exchanges[*].request.uri")
            .value(org.hamcrest.Matchers.everyItem(
                org.hamcrest.Matchers.not(org.hamcrest.Matchers.anyOf(
                    org.hamcrest.Matchers.containsString("/actuator"),
                    org.hamcrest.Matchers.containsString("/observability"))))));
  }

  @Test
  void activityFeedJoinsHttpRequestWithStoredPayment() throws Exception {
    BankResponse bankResponse = new BankResponse();
    bankResponse.setAuthorized(true);
    bankResponse.setAuthorizationCode("auth-code");
    when(restTemplate.postForEntity(anyString(), any(), eq(BankResponse.class)))
        .thenReturn(new ResponseEntity<>(bankResponse, HttpStatus.OK));

    String requestBody = "{\"card_number\":\"2222405343248877\","
        + "\"expiry_month\":12,\"expiry_year\":2030,"
        + "\"currency\":\"GBP\",\"amount\":100,\"cvv\":123}";

    // POST carries the correlation header the journal will join on.
    MvcResult postResult = mvc.perform(MockMvcRequestBuilders.post("/payments")
            .header("Idempotency-Key", "activity-key-1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(header().string("X-Payment-Id",
            matchesPattern("[0-9a-f-]{36}")))
        .andReturn();
    String paymentId = postResult.getResponse().getHeader("X-Payment-Id");

    // GET joins on the id embedded in its path.
    mvc.perform(MockMvcRequestBuilders.get("/payment/" + paymentId))
        .andExpect(status().isOk());

    // A rejected request produces no payment: business cells must stay null.
    // Its idempotency key still shows, since it comes from the request header.
    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .header("Idempotency-Key", "activity-key-rejected")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"card_number\":\"123\",\"expiry_month\":13,"
                + "\"expiry_year\":2020,\"currency\":\"XXX\","
                + "\"amount\":-1,\"cvv\":12}"))
        .andExpect(status().isBadRequest());

    // An unrelated probe is recorded in the journal but never appears in the
    // payment-API activity feed.
    mvc.perform(MockMvcRequestBuilders.get("/not-a-payment-path"))
        .andExpect(status().is4xxClientError());

    mvc.perform(MockMvcRequestBuilders.get("/observability/api/activity"))
        .andExpect(status().isOk())
        // POST 201 row: HTTP and business facts joined via X-Payment-Id.
        .andExpect(jsonPath("$[?(@.paymentId == '" + paymentId + "')].method")
            .value(hasItem("POST")))
        .andExpect(jsonPath("$[?(@.paymentId == '" + paymentId + "')].idempotencyKey")
            .value(hasItem("activity-key-1")))
        // The rejected row keeps its key even without a stored payment.
        .andExpect(jsonPath("$[?(@.idempotencyKey == 'activity-key-rejected')].httpStatus")
            .value(hasItem(400)))
        .andExpect(jsonPath("$[?(@.paymentId == '" + paymentId + "')].paymentStatus")
            .value(hasItem("Authorized")))
        .andExpect(jsonPath("$[?(@.paymentId == '" + paymentId + "')].amount")
            .value(hasItem(100)))
        .andExpect(jsonPath("$[?(@.paymentId == '" + paymentId + "')].currency")
            .value(hasItem("GBP")))
        // GET row for the same payment joins via the path id.
        .andExpect(jsonPath("$[?(@.method == 'GET' "
            + "&& @.path == '/payment/" + paymentId + "')].paymentId")
            .value(hasItem(paymentId)))
        // Every 400 row has no linked payment.
        .andExpect(jsonPath("$[?(@.httpStatus == 400)].paymentStatus")
            .value(everyItem(nullValue())))
        // Only payment-API paths are present.
        .andExpect(jsonPath("$[*].path")
            .value(everyItem(org.hamcrest.Matchers.anyOf(
                org.hamcrest.Matchers.is("/payments"),
                org.hamcrest.Matchers.startsWith("/payment/")))));
  }

  @Test
  void processedPaymentIsCountedByBusinessOutcome() throws Exception {
    BankResponse bankResponse = new BankResponse();
    bankResponse.setAuthorized(true);
    bankResponse.setAuthorizationCode("auth-code");
    when(restTemplate.postForEntity(anyString(), any(), eq(BankResponse.class)))
        .thenReturn(new ResponseEntity<>(bankResponse, HttpStatus.OK));

    String requestBody = "{\"card_number\":\"2222405343248877\","
        + "\"expiry_month\":12,\"expiry_year\":2030,"
        + "\"currency\":\"GBP\",\"amount\":100,\"cvv\":123}";

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isCreated());

    // Exposed as payments_processed_total{status="AUTHORIZED"} by Prometheus;
    // here we assert the underlying Micrometer counter.
    Counter authorized = meterRegistry.find("payments.processed")
        .tag("status", "AUTHORIZED").counter();
    assertThat(authorized).isNotNull();
    assertThat(authorized.count()).isGreaterThanOrEqualTo(1.0);
  }
}
