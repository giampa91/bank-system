package com.bank.system.payment_service.service;

import com.bank.system.dtos.dto.*;
import com.bank.system.payment_service.domain.Payment;
import com.bank.system.payment_service.domain.PaymentStatus;
import com.bank.system.payment_service.domain.ProcessedEvent;
import com.bank.system.payment_service.repository.PaymentRepository;
import com.bank.system.payment_service.repository.ProcessedEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentAccountService {

    private static final Logger log = LoggerFactory.getLogger(PaymentAccountService.class);
    public static final String PAYMENT_INITIATED_EVENT = "PaymentInitiatedEvent";
    public static final String RECEIVER_CREDIT_REQUEST_EVENT = "ReceiverCreditRequestEvent";
    public static final String PAYMENT_COMPLETED_EVENT = "PaymentCompletedEvent";
    public static final String COMPENSATE_PAYMENT_EVENT = "CompensatePaymentEvent";

    public static final String SENDER_DEBITED_FAILED_EVENT = "SenderDebitedFailedEvent";
    public static final String SENDER_DEBITED_EVENT = "SenderDebitedEvent";
    public static final String RECEIVER_CREDIT_EVENT = "ReceiverCreditEvent";
    public static final String RECEIVER_CREDIT_FAILED_EVENT = "ReceiverCreditFailedEvent";

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionalService paymentTransactionalService;
    private final ObjectMapper objectMapper;
    private final ProcessedEventRepository processedEventRepository;

    public PaymentAccountService(PaymentRepository paymentRepository,
                                 PaymentTransactionalService paymentTransactionalService,
                                 ObjectMapper objectMapper,
                                 ProcessedEventRepository processedEventRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentTransactionalService = paymentTransactionalService;
        this.objectMapper = objectMapper;
        this.processedEventRepository = processedEventRepository;
    }

    public Payment initiatePayment(PaymentRequestDTO requestDTO) {
        Optional<Payment> existingPaymentOpt = paymentRepository.findByIdempotencyKeyId(requestDTO.getIdempotencyKey());
        if (existingPaymentOpt.isPresent()) {
            Payment existingPayment = existingPaymentOpt.get();
            log.warn("Payment with idempotencyKey {} already exists with status: {}",
                    requestDTO.getIdempotencyKey(), existingPayment.getStatus());
            return existingPayment;
        } else {
            return paymentTransactionalService.createPaymentAndOutboxEvent(requestDTO);
        }
    }

    private Optional<Payment> updateTransaction(UUID paymentId, PaymentStatus paymentStatus) {
        paymentRepository.findByIdForUpdate(paymentId);
        return paymentRepository.updateStatus(paymentId, paymentStatus);
    }

    public void handleSenderDebited(SenderDebitedEvent event) {
        if (processedEventRepository.existsById(event.getEventId())) {
            return;
        }
        saveProcessedEvent(event, SENDER_DEBITED_EVENT);

        log.info("Received SenderDebitedEvent for paymentId: {}", event.getPaymentId());
        updateTransaction(event.getPaymentId(), PaymentStatus.SENDER_DEBITED)
                .ifPresentOrElse(updatedPayment -> {
                    paymentTransactionalService.updatePaymentReceiverCreditRequestEventAndCreateOutboxEvent(updatedPayment);
                    log.info("Payment {} status updated to SENDER_DEBITED. Next: Credit Receiver.", event.getPaymentId());
                }, () -> log.error("Failed to update payment status to SENDER_DEBITED for paymentId: {}. Payment not found.", event.getPaymentId()));
    }

    public void handleReceiverCredited(ReceiverCreditEvent event) {
        if (processedEventRepository.existsById(event.getEventId())) {
            return;
        }
        saveProcessedEvent(event, RECEIVER_CREDIT_EVENT);

        log.info("Received ReceiverCreditedEvent for paymentId: {}", event.getPaymentId());
        paymentRepository.updateStatus(event.getPaymentId(), PaymentStatus.COMPLETED).ifPresentOrElse(payment -> {
                    log.info("Payment {} status updated to COMPLETED. Publishing PaymentCompletedEvent.", event.getPaymentId());
                    paymentTransactionalService.updatePaymentPaymentCompletedEventAndCreateOutboxEvent(payment);
                }, () -> log.error("Failed to update payment status to COMPLETED for paymentId: {}. Payment not found.", event.getPaymentId())
        );
    }

    public void handleCompensatePayment(CompensatePaymentEvent event) {
        if (processedEventRepository.existsById(event.getEventId())) {
            return;
        }
        saveProcessedEvent(event, COMPENSATE_PAYMENT_EVENT);

        log.info("Received CompensatePaymentEvent for paymentId: {}", event.getPaymentId());
        paymentRepository.updateStatus(event.getPaymentId(), PaymentStatus.COMPLETED).ifPresentOrElse(payment -> {
                    log.info("Payment {} status updated to COMPLETED. Publishing PaymentCompletedEvent.", event.getPaymentId());
                    paymentTransactionalService.updatePaymentPaymentCompletedEventAndCreateOutboxEvent(payment);
                }, () -> log.error("Failed to update payment status to COMPLETED for paymentId: {}. Payment not found.", event.getPaymentId())
        );
    }

    public void handleDebitFailed(DebitFailedEvent event) {
        if (processedEventRepository.existsById(event.getEventId())) {
            return;
        }
        saveProcessedEvent(event, SENDER_DEBITED_FAILED_EVENT);

        log.info("Received DebitFailedEvent for paymentId: {}", event.getPaymentId());
        Optional<Payment> updatedPaymentOpt = paymentRepository.updateStatus(event.getPaymentId(), PaymentStatus.DEBIT_FAILED);
        if (updatedPaymentOpt.isPresent()) {
            log.info("Payment {} status updated to DEBIT_FAILED.", event.getPaymentId());
        } else {
            log.error("Failed to update payment status to DEBIT_FAILED for paymentId: {}. Payment not found.", event.getPaymentId());
        }
    }

    public void handleCreditFailed(CreditFailedEvent event) {
        if (processedEventRepository.existsById(event.getEventId())) {
            return;
        }
        saveProcessedEvent(event, RECEIVER_CREDIT_FAILED_EVENT);

        log.info("Received CreditFailedEvent for paymentId: {}", event.getPaymentId());
        paymentRepository.updateStatus(event.getPaymentId(), PaymentStatus.CREDIT_FAILED).ifPresentOrElse(
                payment -> {
                    log.info("Payment {} status updated to CREDIT_FAILED. Publishing CompensatePaymentEvent.", event.getPaymentId());
                    paymentTransactionalService.updatePaymentCompensatePaymentEventAndCreateOutboxEvent(payment);
                }, () -> log.error("Failed to update payment status to CREDIT_FAILED for paymentId: {}. Payment not found.", event.getPaymentId())
        );
    }

    private void saveProcessedEvent(Event event, String eventType) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize " + eventType, e);
        }
        processedEventRepository.save(new ProcessedEvent(
                event.getEventId(),
                eventType,
                payload,
                Instant.now()
        ));
    }
}
