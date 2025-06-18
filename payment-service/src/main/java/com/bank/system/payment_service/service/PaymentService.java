package com.bank.system.payment_service.service;

import com.bank.system.dtos.dto.PaymentRequestDTO;
import com.bank.system.payment_service.domain.Payment;
import com.bank.system.payment_service.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentAccountService paymentAccountService;
    private final PaymentRepository paymentRepository;
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    public PaymentService(PaymentAccountService paymentAccountService,
                          PaymentRepository paymentRepository) {
        this.paymentAccountService = paymentAccountService;
        this.paymentRepository = paymentRepository;
    }

    public ResponseEntity<Payment> initiatePayment(PaymentRequestDTO requestDTO) {
        log.info("Received payment initiation request for sender: {} to receiver: {} with amount: {}",
                requestDTO.getSenderAccountId(), requestDTO.getReceiverAccountId(), requestDTO.getAmount());
        try {
            Payment payment = paymentAccountService.initiatePayment(requestDTO); // Synchronous call
            log.info("Payment initiated successfully with ID: {}", payment.getId());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(payment);
        } catch (Exception ex) {
            log.error("Error initiating payment: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public Optional<Payment> getPaymentById(UUID paymentId) {
        return paymentRepository.findById(paymentId); // Use a synchronous method
    }
}
