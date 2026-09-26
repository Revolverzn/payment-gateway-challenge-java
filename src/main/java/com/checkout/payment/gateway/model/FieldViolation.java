package com.checkout.payment.gateway.model;

/**
 * A single invalid request field: the API field name and a human-readable
 * reason. Exposed to callers as part of the 400 response body so clients can
 * render field-level errors instead of parsing a free-text message.
 */
public record FieldViolation(String field, String message) {
}
