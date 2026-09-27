package com.checkout.payment.gateway.observability;

import java.util.List;
import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;

/**
 * Decorator over {@link InMemoryHttpExchangeRepository} that keeps
 * observability traffic itself out of the journal.
 *
 * <p>The human-readable dashboard polls the Actuator endpoints every few
 * seconds; without this filter those polls would fill the bounded ring
 * buffer and evict the real payment traffic an operator wants to see. Only
 * business requests are recorded — all persistence stays delegated to the
 * stock in-memory repository.
 */
public class FilteredHttpExchangeRepository implements HttpExchangeRepository {

  // /observability covers both the dashboard page and its /observability/api
  // polling feed; neither belongs in the business journal.
  private static final List<String> IGNORED_PREFIXES = List.of("/actuator", "/observability");
  private static final List<String> IGNORED_PATHS = List.of("/favicon.ico");

  private final InMemoryHttpExchangeRepository delegate;

  public FilteredHttpExchangeRepository(InMemoryHttpExchangeRepository delegate) {
    this.delegate = delegate;
  }

  @Override
  public void add(HttpExchange exchange) {
    String path = exchange.getRequest().getUri().getPath();
    if (IGNORED_PREFIXES.stream().anyMatch(path::startsWith)
        || IGNORED_PATHS.contains(path)) {
      return;
    }
    delegate.add(exchange);
  }

  @Override
  public List<HttpExchange> findAll() {
    return delegate.findAll();
  }
}
