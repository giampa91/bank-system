package com.bank.system.payment_service.controller;

import com.bank.system.dtos.dto.*;
import com.bank.system.payment_service.domain.Payment;
import com.bank.system.payment_service.service.PaymentAccountService;
import com.bank.system.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/payments")
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
        return paymentService.initiatePayment(requestDTO);
    }

    // TODO: Add endpoints for getting payment status, history (if not handled by Transaction Service)
}
