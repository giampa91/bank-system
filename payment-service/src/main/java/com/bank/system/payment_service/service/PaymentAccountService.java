package com.bank.system.payment_service.service;

import com.bank.system.dtos.dto.*;
import com.bank.system.payment_service.domain.OutboxEvent;
import com.bank.system.payment_service.domain.Payment;
import com.bank.system.payment_service.domain.PaymentStatus;
import com.bank.system.payment_service.kafka.PaymentProducer;
import com.bank.system.payment_service.repository.OutboxEventRepository;
import com.bank.system.payment_service.repository.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentAccountService {

    private static final Logger log = LoggerFactory.getLogger(PaymentAccountService.class);

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentProducer paymentProducer;
    private final ObjectMapper objectMapper;

    public PaymentAccountService(PaymentRepository paymentRepository, OutboxEventRepository outboxEventRepository,
                                 PaymentProducer paymentProducer, ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.paymentProducer = paymentProducer;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Payment initiatePayment(PaymentRequestDTO requestDTO) {
        Optional<Payment> existingPaymentOpt = paymentRepository.findByIdempotencyKeyId(requestDTO.getIdempotencyKey());
        if (existingPaymentOpt.isPresent()) {
            Payment existingPayment = existingPaymentOpt.get();
            log.warn("Payment with idempotencyKey {} already exists with status: {}",
                    requestDTO.getIdempotencyKey(), existingPayment.getStatus());
            return existingPayment;
        } else {
            Payment payment = mapPaymentRequestDtoToPayment(requestDTO);
            Payment savedPayment = paymentRepository.save(payment);
            PaymentInitiatedEvent event = mapToPaymentInitiatedEvent(savedPayment);
            String eventPayload;
            try {
                eventPayload = objectMapper.writeValueAsString(event);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize PaymentInitiatedEvent for paymentId {}: {}", savedPayment.getId(), e.getMessage(), e);
                throw new RuntimeException("Failed to serialize PaymentInitiatedEvent", e);
            }
            saveEvent(savedPayment, eventPayload, "PaymentInitiatedEvent");
            log.info("Payment initiated and event saved for paymentId: {}", savedPayment.getId());
            return savedPayment;
        }
    }

    @Transactional
    public void handleSenderDebited(SenderDebitedEvent senderDebitedEvent) {
        log.info("Received SenderDebitedEvent for paymentId: {}", senderDebitedEvent.getPaymentId());
        Optional<Payment> updatedPaymentOpt = paymentRepository.updateStatus(senderDebitedEvent.getPaymentId(), PaymentStatus.SENDER_DEBITED);

        if (updatedPaymentOpt.isPresent()) {
            Payment payment = updatedPaymentOpt.get();
            ReceiverCreditRequestEvent event = mapPaymentToReceiverCreditRequestEvent(payment);
            String eventPayload;
            try {
                eventPayload = objectMapper.writeValueAsString(event);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize ReceiverCreditRequestEvent for paymentId {}: {}", payment.getId(), e.getMessage(), e);
                throw new RuntimeException("Failed to serialize ReceiverCreditRequestEvent", e);
            }
            saveEvent(payment, eventPayload, "ReceiverCreditRequestEvent");
            log.info("Payment {} status updated to SENDER_DEBITED. Next: Credit Receiver.", senderDebitedEvent.getPaymentId());
        } else {
            log.error("Failed to update payment status to SENDER_DEBITED for paymentId: {}. Payment not found.", senderDebitedEvent.getPaymentId());
        }
    }

    @Transactional
    public void handleReceiverCredited(ReceiverCreditEvent event) {
        log.info("Received ReceiverCreditedEvent for paymentId: {}", event.getPaymentId());
        Optional<Payment> updatedPaymentOpt = paymentRepository.updateStatus(event.getPaymentId(), PaymentStatus.COMPLETED);
        if (updatedPaymentOpt.isPresent()) {
            Payment payment = updatedPaymentOpt.get();
            log.info("Payment {} status updated to COMPLETED. Publishing PaymentCompletedEvent.", event.getPaymentId());
            sendPaymentCompletedEvent(payment); // Call synchronous helper
        } else {
            log.error("Failed to update payment status to COMPLETED for paymentId: {}. Payment not found.", event.getPaymentId());
        }
    }

    @Transactional
    public void handleCompensatePayment(CompensatePaymentEvent event) {
        log.info("Received CompensatePaymentEvent for paymentId: {}", event.getPaymentId());
        Optional<Payment> updatedPaymentOpt = paymentRepository.updateStatus(event.getPaymentId(), PaymentStatus.COMPLETED);

        if (updatedPaymentOpt.isPresent()) {
            Payment payment = updatedPaymentOpt.get();
            log.info("Payment {} status updated to COMPLETED. Publishing PaymentCompletedEvent.", event.getPaymentId());
            sendPaymentCompletedEvent(payment); // Call synchronous helper
        } else {
            log.error("Failed to update payment status to COMPLETED for paymentId: {}. Payment not found.", event.getPaymentId());
        }
    }

    @Transactional
    public void handleDebitFailed(DebitFailedEvent event) {
        log.info("Received DebitFailedEvent for paymentId: {}", event.getPaymentId());
        Optional<Payment> updatedPaymentOpt = paymentRepository.updateStatus(event.getPaymentId(), PaymentStatus.DEBIT_FAILED);
        if (updatedPaymentOpt.isPresent()) {
            log.info("Payment {} status updated to DEBIT_FAILED.", event.getPaymentId());
        } else {
            log.error("Failed to update payment status to DEBIT_FAILED for paymentId: {}. Payment not found.", event.getPaymentId());
        }
    }

    @Transactional
    public void handleCreditFailed(CreditFailedEvent event) {
        log.info("Received CreditFailedEvent for paymentId: {}", event.getPaymentId());
        Optional<Payment> updatedPaymentOpt = paymentRepository.updateStatus(event.getPaymentId(), PaymentStatus.CREDIT_FAILED);
        if (updatedPaymentOpt.isPresent()) {
            Payment payment = updatedPaymentOpt.get();
            log.info("Payment {} status updated to CREDIT_FAILED. Publishing CompensatePaymentEvent.", event.getPaymentId());
            sendCompensatePaymentEventForCreditFailed(payment);
        } else {
            log.error("Failed to update payment status to CREDIT_FAILED for paymentId: {}. Payment not found.", event.getPaymentId());
        }
    }

    private void sendPaymentCompletedEvent(Payment payment) {
        PaymentCompletedEvent event = mapPaymentToPaymentCompletedEvent(payment);
        try {
            String eventPayload;
            try {
                eventPayload = objectMapper.writeValueAsString(event);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize PaymentCompletedEvent for paymentId {}: {}", payment.getId(), e.getMessage(), e);
                throw new RuntimeException("Failed to serialize PaymentCompletedEvent", e);
            }
            saveEvent(payment, eventPayload, "PaymentCompletedEvent");
            log.info("PaymentCompletedEvent published for paymentId: {}", payment.getId());
        } catch (Exception ex) {
            log.error("Failed to publish PaymentCompletedEvent for paymentId {}: {}", payment.getId(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to publish PaymentCompletedEvent", ex);
        }
    }

    private void sendCompensatePaymentEventForCreditFailed(Payment payment) {
        CompensatePaymentEvent event = mapPaymentToCompensatePaymentEvent(payment);
        try {
            String eventPayload;
            try {
                eventPayload = objectMapper.writeValueAsString(event);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize CompensatePaymentEvent for paymentId {}: {}", payment.getId(), e.getMessage(), e);
                throw new RuntimeException("Failed to serialize PaymentInitiatedEvent", e);
            }
            saveEvent(payment, eventPayload, "CompensatePaymentEvent");
            log.info("CompensatePaymentEvent published for paymentId: {}", payment.getId());
        } catch (Exception ex) {
            log.error("Failed to publish CompensatePaymentEvent for paymentId {}: {}", payment.getId(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to publish CompensatePaymentEvent", ex);
        }
    }

    private void saveEvent(Payment savedPayment, String payload, String type) {
        OutboxEvent outboxEvent = new OutboxEvent(
                UUID.randomUUID(),
                "Payment",
                savedPayment.getId(),
                type,
                payload,
                Instant.now(),
                false
        );
        outboxEventRepository.save(outboxEvent);
        log.debug("Outbox event saved for paymentId: {}", savedPayment.getId());
    }

    private static PaymentCompletedEvent mapPaymentToPaymentCompletedEvent(Payment payment) {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setPaymentId(payment.getId());
        event.setSenderAccountId(payment.getSenderAccountId());
        event.setReceiverAccountId(payment.getReceiverAccountId());
        event.setAmount(payment.getAmount());
        event.setCurrency(payment.getCurrency());
        event.setTimestamp(Instant.now());
        return event;
    }

    private static CompensatePaymentEvent mapPaymentToCompensatePaymentEvent(Payment payment) {
        CompensatePaymentEvent event = new CompensatePaymentEvent();
        event.setPaymentId(payment.getId());
        event.setAccountId(payment.getSenderAccountId());
        event.setAmount(payment.getAmount());
        event.setTimestamp(Instant.now());
        return event;
    }

    private static Payment mapPaymentRequestDtoToPayment(PaymentRequestDTO requestDTO) {
        Payment payment = new Payment();
        payment.setSenderAccountId(requestDTO.getSenderAccountId());
        payment.setReceiverAccountId(requestDTO.getReceiverAccountId());
        payment.setAmount(requestDTO.getAmount());
        payment.setCurrency(requestDTO.getCurrency());
        payment.setIdempotencyKey(requestDTO.getIdempotencyKey());
        payment.setStatus(PaymentStatus.INITIATED);
        return payment;
    }

    private static PaymentInitiatedEvent mapToPaymentInitiatedEvent(Payment savedPayment) {
        PaymentInitiatedEvent event = new PaymentInitiatedEvent();
        event.setPaymentId(savedPayment.getId());
        event.setSenderAccountId(savedPayment.getSenderAccountId());
        event.setReceiverAccountId(savedPayment.getReceiverAccountId());
        event.setAmount(savedPayment.getAmount());
        event.setCurrency(savedPayment.getCurrency());
        event.setIdempotencyKey(savedPayment.getIdempotencyKey());
        event.setTimestamp(Instant.now());
        return event;
    }

    private static ReceiverCreditRequestEvent mapPaymentToReceiverCreditRequestEvent(Payment payment) {
        ReceiverCreditRequestEvent receiverCreditRequestEvent = new ReceiverCreditRequestEvent();
        receiverCreditRequestEvent.setAccountId(payment.getReceiverAccountId());
        receiverCreditRequestEvent.setCurrency(payment.getCurrency());
        receiverCreditRequestEvent.setPaymentId(payment.getId());
        receiverCreditRequestEvent.setCreditedAmount(payment.getAmount());
        return receiverCreditRequestEvent;
    }
}
