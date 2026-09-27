package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController("api")
@Tag(name = "Payments", description = "Process card payments and retrieve stored payments")
public class PaymentGatewayController {

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @Operation(summary = "Process a payment",
      description = "Validates the request, calls the acquiring bank and stores the payment. "
          + "A rejected (invalid) request never reaches the bank. An optional Idempotency-Key "
          + "header makes safe retries return the first result.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Payment processed by the bank "
          + "(status Authorized or Declined)"),
      @ApiResponse(responseCode = "400", description = "Validation failed; field-level "
          + "errors are returned, and the bank was never called",
          content = @Content(schema = @Schema(implementation =
              com.checkout.payment.gateway.model.ValidationErrorResponse.class))),
      @ApiResponse(responseCode = "409", description = "Idempotency-Key reused for a "
          + "different payment request"),
      @ApiResponse(responseCode = "502", description = "Bank rejected the gateway request"),
      @ApiResponse(responseCode = "503", description = "Bank unavailable, timed out or "
          + "returned an unusable response")
  })
  @PostMapping("/payments")
  public ResponseEntity<PostPaymentResponse> processPayment(
      @RequestBody PostPaymentRequest paymentRequest,
      @Parameter(description = "Optional client-generated key; identical keys replay the "
          + "first stored result instead of calling the bank again")
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    PostPaymentResponse response =
        paymentGatewayService.processPayment(paymentRequest, idempotencyKey);
    // Lets clients (and the observability journal, which records response
    // headers) correlate this HTTP call with the stored payment resource,
    // including idempotent replays of the same payment.
    return ResponseEntity.status(HttpStatus.CREATED)
        .header("X-Payment-Id", response.getId().toString())
        .body(response);
  }

  @Operation(summary = "Retrieve a previously processed payment")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Payment found"),
      @ApiResponse(responseCode = "400", description = "Path parameter is not a valid UUID"),
      @ApiResponse(responseCode = "404", description = "No payment exists with that id")
  })
  @GetMapping("/payment/{id}")
  public ResponseEntity<PostPaymentResponse> getPostPaymentEventById(
      @Parameter(description = "Payment identifier", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
      @PathVariable UUID id) {
    return new ResponseEntity<>(paymentGatewayService.getPaymentById(id), HttpStatus.OK);
  }
}
