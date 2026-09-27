# Payment Gateway (Java)

A Spring Boot payment gateway that validates card payments, sends them to an
acquiring bank simulator, stores the result and lets merchants retrieve it.
The processing pipeline is orchestrated with LiteFlow, and observability is
provided by Spring Boot Actuator and Micrometer.

## Requirements

- JDK 17
- Docker (for the bank simulator)

## Running locally

```bash
docker-compose up            # Mountebank bank simulator on http://localhost:8080
./gradlew bootRun            # payment gateway on http://localhost:8090
```

Useful endpoints:

| Endpoint | URL |
| --- | --- |
| Observability dashboard (human-readable) | http://localhost:8090/observability.html |
| Swagger UI | http://localhost:8090/swagger-ui/index.html |
| Health check | http://localhost:8090/actuator/health |
| HTTP exchange journal | http://localhost:8090/actuator/httpexchanges |
| Metrics (JSON) | http://localhost:8090/actuator/metrics |
| Prometheus scrape | http://localhost:8090/actuator/prometheus |

## Sample requests

The simulator authorizes cards ending in an odd digit and declines even digits.

Authorized payment:

```bash
curl --location 'http://localhost:8090/payments' \
--header 'Content-Type: application/json' \
--header 'Idempotency-Key: 4' \
--data '{
    "amount": 1000,
    "currency": "GBP",
    "card_number": "4242424242424243",
    "expiry_month": 12,
    "expiry_year": 2028,
    "cvv": "123",
    "merchant_id": "merchant-001"
}'
```

```json
{
    "id": "c9888ff5-dc1d-4eaf-a52e-0f9b9a01d918",
    "status": "Authorized",
    "cardNumberLastFour": 4243,
    "expiryMonth": 12,
    "expiryYear": 2028,
    "currency": "GBP",
    "amount": 1000,
    "authorizationCode": "344c285f-dd6b-43ed-9549-1fa3e8a77189"
}
```

Declined payment (even last digit):

```bash
curl --location 'http://localhost:8090/payments' \
--header 'Content-Type: application/json' \
--header 'Idempotency-Key: 2' \
--data '{
    "amount": 2000,
    "currency": "GBP",
    "card_number": "4242424242424244",
    "expiry_month": 12,
    "expiry_year": 2028,
    "cvv": "123",
    "merchant_id": "merchant-001"
}'
```

```json
{
    "id": "bca4ab0d-e6ba-4b76-9a4e-ab6935ca8efd",
    "status": "Declined",
    "cardNumberLastFour": 4244,
    "expiryMonth": 12,
    "expiryYear": 2028,
    "currency": "GBP",
    "amount": 2000
}
```

Retrieve a payment:

```bash
curl --location 'http://localhost:8090/payment/cf24f1c9-e7e6-4c5e-8cc1-882ae76628f8'
```

A missing payment returns 404 with the specific id:

```json
{
    "message": "Payment not found with id 00000000-1111-2222-3333-000000000000"
}
```

## API

### `POST /payments` — process a payment

Optionally send an `Idempotency-Key` header to retry safely. The same key with
the same payment attributes returns the stored result without calling the bank
again; the same key with different attributes is rejected with `409`.

| Status | Meaning |
| --- | --- |
| `201 Created` | Payment processed; `status` is `Authorized` or `Declined` (also returned for an idempotent replay) |
| `400 Bad Request` | Validation failed; the bank was never called. Field-level errors are returned as `{"errors":[{"field":"...","message":"..."}]}` |
| `409 Conflict` | Idempotency key reused for a different request, or the first attempt is still in flight |
| `502 Bad Gateway` | The bank rejected the gateway's request |
| `503 Service Unavailable` | The bank is unavailable, timed out, or returned an unusable response |

### `GET /payment/{id}` — retrieve a payment

Returns the stored payment (`200`) or `404` for an unknown id. The full card
number and CVV are never stored or returned; only the last four PAN digits are
exposed.

## Architecture

```
Controller
   └─ PaymentGatewayService
        └─ LiteFlow chain (paymentProcessChain)
             validatePayment          strategy-based field validation (amount, card, currency, CVV, expiry)
             checkIdempotency         replay / conflict / proceed
             assembleBankRequest      merchant DTO -> bank contract DTO (expiry MM/yyyy, CVV as string)
             createPaymentOrder       write-ahead: persist INIT record before the bank call
             invokeBank               HTTP call with connect/read timeouts
             parseBankResponse        status -> Authorized/Declined, auth code 
             updatePaymentOrder       write back final status and authorization code
             auditPayment             terminal-state guard, audit log, payments.processed 
        └─ PaymentsRepository (in-memory, unique idempotency-key index)
```

- **Validation** is a set of pluggable `PaymentValidationRule` strategies, so
  each rule is independently testable and the violation list maps directly to
  the structured `400` response.
- **Write-ahead record**: the payment is persisted in the local `Init` state
  *before* the bank is called. A timeout or crash leaves a traceable in-doubt
  payment instead of vanishing without a record. `Init` means "no bank answer
  yet"; `Pending` is reserved for an upstream "still processing" answer, which
  the current simulator never returns.
- **Idempotency** is opt-in via the header. The key alone is not trusted: its
  stored payment attributes must match, otherwise the reuse is a conflict.
- **Contract isolation**: `BankPaymentRequest` is separate from the merchant
  DTO, so the bank's wire format (e.g. `expiry_date` as `"12/2028"`) can change
  without breaking the merchant API.

## Observability

Open `http://localhost:8090/observability.html` for a live dashboard: cards
with success rate, outcome counts and bank-call latency, plus one activity
table where each row is an HTTP request joined with the payment it produced
(id, status, amount, currency, idempotency key). The page is static and
collects nothing — it polls framework-provided endpoints every 3 seconds:

- request timing and the HTTP journal come for free from Spring Boot
  Actuator/Micrometer (`http.server.requests`, bank-call latency from
  `http.client.requests`);
- the join is served by `/observability/api/activity`: POST responses carry
  an `X-Payment-Id` header (also returned on idempotent replays) and GET uses
  the id in its path, so each HTTP row links to its stored payment exactly;
- the journal stores request lines and headers only, never bodies, and only
  the last four card digits are retained — full PAN and CVV stay out.

## Design decisions

- **201 for both Authorized and Declined.** A payment resource is created and
  stored in both cases. The HTTP status describes the gateway result (resource
  created); the bank result lives in the `status` field.
- **400 with structured field errors.** Invalid input is rejected before the
  bank is ever called, so no charge is attempted, and the client receives
  per-field messages rather than one generic failure.
- **Separate bank DTO.** Bank and merchant contracts evolve independently;
  bank-specific formatting stays inside the bank request mapper.
- **Idempotency key plus attribute matching.** A key reused for a genuinely
  different payment must not silently return the wrong stored response.
- **Only one line of custom metric code.** The terminal audit node increments
  a Micrometer counter; everything else (HTTP server/client timers, health,
  the exchange journal, the Prometheus registry) is auto-configured by Spring
  Boot.

## Testing

```bash
./gradlew test       # unit + MockMvc tests, plus JaCoCo report under build/reports/jacoco
./gradlew check      # also enforces 80% line / 70% branch coverage
```

| Layer | What it covers |
| --- | --- |
| Pure unit | Validation rules, bank request mapping, request DTO behaviour |
| Client | Bank contract payload, response parsing, 4xx/5xx/timeout mapping |
| MockMvc (`@SpringBootTest`) | Full HTTP flow: validation, idempotency, bank outcomes, 4xx/5xx paths, observability endpoints |

CI runs `./gradlew clean build` on every push and pull request and uploads the
JaCoCo report as a build artifact.

## Assumptions and limitations

- Storage is in-memory (`ConcurrentHashMap`); restarting the service clears
  all payments and the exchange journal. A production system would use a
  durable store and a metrics backend (Prometheus/Grafana).
- Supported currencies are USD, GBP and EUR.
- Observability counters and the exchange journal are process-local; they are
  meant for local inspection and demos, not as a production monitoring stack.

