package com.bank.system.payment_service.service;

import com.bank.system.dtos.dto.*;
import com.bank.system.payment_service.domain.OutboxEvent;
import com.bank.system.payment_service.domain.Payment;
import com.bank.system.payment_service.mapper.PaymentMapper;
import com.bank.system.payment_service.repository.OutboxEventRepository;
import com.bank.system.payment_service.repository.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

import static com.bank.system.payment_service.service.PaymentAccountService.*;

@Service
public class PaymentTransactionalService {

    private static final Logger log = LoggerFactory.getLogger(PaymentAccountService.class);
    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public PaymentTransactionalService(PaymentRepository paymentRepository,
                                       OutboxEventRepository outboxEventRepository,
                                       ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Payment createPaymentAndOutboxEvent(PaymentRequestDTO requestDTO) {
        Payment payment = PaymentMapper.mapPaymentRequestDtoToPayment(requestDTO);
        Payment savedPayment = paymentRepository.save(payment);

        PaymentInitiatedEvent event = PaymentMapper.mapToPaymentInitiatedEvent(savedPayment);
        String eventPayload;

        try {
            eventPayload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize " + PAYMENT_INITIATED_EVENT, e);
        }

        outboxEventRepository.save(new OutboxEvent(
                UUID.randomUUID(),
                "Payment",
                savedPayment.getId(),
                PAYMENT_INITIATED_EVENT,
                eventPayload,
                Instant.now(),
                false
        ));

        return savedPayment;
    }

    @Transactional
    public void updatePaymentReceiverCreditRequestEventAndCreateOutboxEvent(Payment payment) {
        ReceiverCreditRequestEvent event = PaymentMapper.mapPaymentToReceiverCreditRequestEvent(payment);
        String eventPayload;
        try {
            eventPayload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ReceiverCreditRequestEvent for paymentId {}: {}", payment.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to serialize ReceiverCreditRequestEvent", e);
        }
        saveEvent(payment, eventPayload, RECEIVER_CREDIT_REQUEST_EVENT);
    }

    @Transactional
    public void updatePaymentPaymentCompletedEventAndCreateOutboxEvent(Payment payment) {
        PaymentCompletedEvent event = PaymentMapper.mapPaymentToPaymentCompletedEvent(payment);
        try {
            String eventPayload;
            try {
                eventPayload = objectMapper.writeValueAsString(event);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize PaymentCompletedEvent for paymentId {}: {}", payment.getId(), e.getMessage(), e);
                throw new RuntimeException("Failed to serialize PaymentCompletedEvent", e);
            }
            saveEvent(payment, eventPayload, PAYMENT_COMPLETED_EVENT);
            log.info("PaymentCompletedEvent published for paymentId: {}", payment.getId());
        } catch (Exception ex) {
            log.error("Failed to publish PaymentCompletedEvent for paymentId {}: {}", payment.getId(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to publish PaymentCompletedEvent", ex);
        }
    }

    @Transactional
    public void updatePaymentCompensatePaymentEventAndCreateOutboxEvent(Payment payment) {
        CompensatePaymentEvent event = PaymentMapper.mapPaymentToCompensatePaymentEvent(payment);
        try {
            String eventPayload;
            try {
                eventPayload = objectMapper.writeValueAsString(event);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize CompensatePaymentEvent for paymentId {}: {}", payment.getId(), e.getMessage(), e);
                throw new RuntimeException("Failed to serialize PaymentInitiatedEvent", e);
            }
            saveEvent(payment, eventPayload, COMPENSATE_PAYMENT_EVENT);
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
}

