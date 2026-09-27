package com.checkout.payment.gateway.configuration;

import com.checkout.payment.gateway.observability.FilteredHttpExchangeRepository;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ApplicationConfiguration {

  /**
   * Backs the {@code /actuator/httpexchanges} endpoint: an in-memory ring
   * buffer of the most recent business HTTP exchanges (request
   * line/timestamp, response status, latency, sanitized headers). The
   * dashboard's own Actuator polling is not recorded, so it cannot evict
   * real payment traffic. Request bodies are not retained, so the full PAN
   * and CVV never enter the journal.
   *
   * <p>The buffer is deliberately bounded (each entry retains request and
   * response headers, so an unbounded journal would leak memory); the
   * capacity is configurable via {@code observability.http-exchanges.capacity}.
   */
  @Bean
  public HttpExchangeRepository httpExchangeRepository(
      @Value("${observability.http-exchanges.capacity:1000}") int capacity) {
    InMemoryHttpExchangeRepository repository = new InMemoryHttpExchangeRepository();
    repository.setCapacity(capacity);
    return new FilteredHttpExchangeRepository(repository);
  }

  @Bean
  public RestTemplate restTemplate(
      RestTemplateBuilder builder,
      @Value("${bank.connect-timeout-ms:2000}") int connectTimeoutMs,
      @Value("${bank.read-timeout-ms:5000}") int readTimeoutMs) {
    return builder
        // A bank call must never hold a request thread indefinitely.
        .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
        .setReadTimeout(Duration.ofMillis(readTimeoutMs))
        .build();
  }
}
