# Instructions for candidates

This is the Java version of the Payment Gateway challenge. If you haven't already read this [README.md](https://github.com/cko-recruitment/) on the details of this exercise, please do so now.

## Requirements
- JDK 17
- Docker

## Template structure

src/ - A skeleton SpringBoot Application

test/ - Some simple JUnit tests

imposters/ - contains the bank simulator configuration. Don't change this

.editorconfig - don't change this. It ensures a consistent set of rules for submissions when reformatting code

docker-compose.yml - configures the bank simulator


## API Documentation
For documentation openAPI is included, and it can be found under the following url: **http://localhost:8090/swagger-ui/index.html**

**Feel free to change the structure of the solution, use a different library etc.**

## Running it

Start the bank simulator, then the gateway:

```bash
docker-compose up            # Mountebank bank simulator on http://localhost:8080
./gradlew bootRun            # payment gateway on http://localhost:8090
```

Run the tests:

```bash
./gradlew test
```

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

```json
{
    "id": "cf24f1c9-e7e6-4c5e-8cc1-882ae76628f8",
    "status": "Authorized",
    "cardNumberLastFour": 4243,
    "expiryMonth": 12,
    "expiryYear": 2028,
    "currency": "GBP",
    "amount": 1000,
    "authorizationCode": "f48d7bd0-3b8b-477f-bbd8-53f132b70732"
}
```

A missing payment returns 404:

```json
{
    "message": "Payment not found with id 00000000-1111-2222-3333-000000000000"
}
```
