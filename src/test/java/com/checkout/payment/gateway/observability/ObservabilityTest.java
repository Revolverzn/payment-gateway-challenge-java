package com.checkout.payment.gateway.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.client.BankResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

// Boot Test disables metric exporters and tracing by default (in-memory
// SimpleMeterRegistry, no publishing); opt into the real metrics registry so
// the Prometheus wiring and /actuator/prometheus are exercised as in production.
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability(metrics = true)
class ObservabilityTest {

  @Autowired
  private MockMvc mvc;
  @MockBean
  private RestTemplate restTemplate;

  @Test
  void healthEndpointReportsUp() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(result -> assertThat(result.getResponse().getContentAsString())
            .contains("\"status\":\"UP\""));
  }

  @Test
  void processedPaymentAndReplayAreRecordedAsMetricsAndExposedToPrometheus() throws Exception {
    BankResponse bankResponse = new BankResponse();
    bankResponse.setAuthorized(true);
    bankResponse.setAuthorizationCode("auth-code");
    when(restTemplate.postForEntity(anyString(), any(), eq(BankResponse.class)))
        .thenReturn(new ResponseEntity<>(bankResponse, HttpStatus.OK));

    String body = "{\"card_number\":\"2222405343248877\","
        + "\"expiry_month\":12,\"expiry_year\":2030,"
        + "\"currency\":\"GBP\",\"amount\":100,\"cvv\":123}";
    String key = UUID.randomUUID().toString();

    mvc.perform(post("/payments").header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated());
    mvc.perform(post("/payments").header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated());

    mvc.perform(get("/actuator/metrics/payments.processed")
            .param("tag", "status:Authorized"))
        .andExpect(status().isOk())
        .andExpect(result -> {
          String processedJson = result.getResponse().getContentAsString();
          assertThat(processedJson).contains("payments.processed");
          // The Authorized counter must have observed the payment above (> 0).
          assertThat(processedJson).doesNotContain("\"value\":0.0");
        });

    mvc.perform(get("/actuator/metrics/payments.replayed"))
        .andExpect(status().isOk())
        .andExpect(result -> {
          String replayJson = result.getResponse().getContentAsString();
          assertThat(replayJson).contains("payments.replayed");
          assertThat(replayJson).doesNotContain("\"value\":0.0");
        });

    mvc.perform(get("/actuator/prometheus"))
        .andExpect(status().isOk())
        .andExpect(result -> {
          String prometheusText = result.getResponse().getContentAsString();
          assertThat(prometheusText)
              .contains("payments_processed_total")
              .contains("payments_replayed_total");
        });
  }
}
