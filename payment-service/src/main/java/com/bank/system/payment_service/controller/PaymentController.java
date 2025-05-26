package com.bank.system.payment_service.controller;

import com.bank.system.payment_service.domain.Payment;
import com.bank.system.payment_service.dto.PaymentRequestDTO;
import com.bank.system.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * REST endpoint to initiate a new payment.
     * The API Gateway will route requests to this endpoint.
     *
     * @param requestDTO The payment request details.
     * @return A CompletableFuture that completes with a ResponseEntity containing the initiated Payment.
     */
    @PostMapping("/initiate")
    public CompletableFuture<ResponseEntity<Payment>> initiatePayment(@Valid @RequestBody PaymentRequestDTO requestDTO) {
        log.info("Received payment initiation request for sender: {} to receiver: {} with amount: {}",
                requestDTO.getSenderAccountId(), requestDTO.getReceiverAccountId(), requestDTO.getAmount());

        return paymentService.initiatePayment(requestDTO)
                .thenApply(payment -> {
                    log.info("Payment initiated successfully with ID: {}", payment.getPaymentId());
                    return ResponseEntity.status(HttpStatus.ACCEPTED).body(payment);
                })
                .exceptionally(ex -> {
                    log.error("Error initiating payment: {}", ex.getMessage(), ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }

    // TODO: Add endpoints for getting payment status, history (if not handled by Transaction Service)
}
