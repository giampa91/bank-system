package com.bank.system.payment_service.controller;

import com.bank.system.dtos.dto.PaymentRequestDTO;
import com.bank.system.payment_service.domain.Payment;
import com.bank.system.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<Payment> initiatePayment(@Valid @RequestBody PaymentRequestDTO requestDTO) {
        return paymentService.initiatePayment(requestDTO);
    }

    // TODO: Add endpoints for getting payment status, history (if not handled by Transaction Service)
}
