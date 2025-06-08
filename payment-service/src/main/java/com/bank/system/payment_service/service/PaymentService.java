package com.bank.system.payment_service.service;

import com.bank.system.dtos.dto.PaymentRequestDTO;
import com.bank.system.payment_service.controller.PaymentController;
import com.bank.system.payment_service.domain.Payment;
import com.bank.system.payment_service.repository.PaymentRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class PaymentService {

    private PaymentAccountService paymentAccountService;
    private PaymentRepository paymentRepository;
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    public PaymentService(PaymentAccountService paymentAccountService,
                          PaymentRepository paymentRepository) {
        this.paymentAccountService = paymentAccountService;
        this.paymentRepository = paymentRepository;
    }

    public CompletableFuture<ResponseEntity<Payment>> initiatePayment(PaymentRequestDTO requestDTO) {
        log.info("Received payment initiation request for sender: {} to receiver: {} with amount: {}",
                requestDTO.getSenderAccountId(), requestDTO.getReceiverAccountId(), requestDTO.getAmount());
        return paymentAccountService.initiatePayment(requestDTO)
                .thenApply(payment -> {
                    log.info("Payment initiated successfully with ID: {}", payment.getId());
                    return ResponseEntity.status(HttpStatus.ACCEPTED).body(payment);
                })
                .exceptionally(ex -> {
                    log.error("Error initiating payment: {}", ex.getMessage(), ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }

    public CompletableFuture<Optional<Payment>> getPaymentById(UUID paymentId) {
        return paymentRepository.findById(paymentId);
    }
}
