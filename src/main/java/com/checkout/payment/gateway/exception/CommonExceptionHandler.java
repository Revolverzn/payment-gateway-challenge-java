package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.ErrorResponse;
import com.checkout.payment.gateway.model.ValidationErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(EventProcessingException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(EventProcessingException ex) {
    // A missing payment is an expected business outcome; no stack trace needed.
    // Surface the real reason (including the payment id) instead of a generic
    // web-style "page not found" message — API callers need to know which
    // resource was missing.
    LOG.debug("Payment not found: {}", ex.getMessage());
    return new ResponseEntity<>(new ErrorResponse(ex.getMessage()),
        HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(PaymentValidationException.class)
  public ResponseEntity<ValidationErrorResponse> handleValidation(PaymentValidationException ex) {
    return new ResponseEntity<>(new ValidationErrorResponse(ex.getViolations()),
        HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(IdempotencyConflictException.class)
  public ResponseEntity<ErrorResponse> handleIdempotencyConflict(
      IdempotencyConflictException ex) {
    return new ResponseEntity<>(new ErrorResponse(ex.getMessage()), HttpStatus.CONFLICT);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMalformedBody(HttpMessageNotReadableException ex) {
    return new ResponseEntity<>(new ErrorResponse("Malformed request body"),
        HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    return new ResponseEntity<>(new ErrorResponse("Invalid parameter: " + ex.getName()),
        HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(UpstreamClientException.class)
  public ResponseEntity<ErrorResponse> handleUpstreamClientError(UpstreamClientException ex) {
    return new ResponseEntity<>(new ErrorResponse(ex.getMessage()),
        HttpStatus.BAD_GATEWAY);
  }

  @ExceptionHandler(BankServiceException.class)
  public ResponseEntity<ErrorResponse> handleBankUnavailable(BankServiceException ex) {
    LOG.error("Bank service exception", ex);
    return new ResponseEntity<>(new ErrorResponse(ex.getMessage()),
        HttpStatus.SERVICE_UNAVAILABLE);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
    // Last line of defence: never leak framework internals/stack traces to clients.
    LOG.error("Unexpected error", ex);
    return new ResponseEntity<>(new ErrorResponse("Internal server error"),
        HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
