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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
    public CompletableFuture<Payment> initiatePayment(PaymentRequestDTO requestDTO) {
        return paymentRepository.findByIdempotencyKeyId(requestDTO.getIdempotencyKey())
                .thenCompose(existingPaymentOpt -> {
                    if (existingPaymentOpt.isPresent()) {
                        Payment existingPayment = existingPaymentOpt.get();
                        log.warn("Payment with idempotencyKey {} already exists with status: {}",
                                requestDTO.getIdempotencyKey(), existingPayment.getStatus());
                        return CompletableFuture.completedFuture(existingPayment);
                    } else {
                        Payment payment = mapPaymentRequestDtoToPayment(requestDTO);
                        return paymentRepository.save(payment)
                                .thenCompose(savedPayment -> {
                                    PaymentInitiatedEvent event = mapToPaymentInitiatedEvent(savedPayment);
                                    String eventPayload = null;
                                    try {
                                        eventPayload = objectMapper.writeValueAsString(event);
                                    } catch (JsonProcessingException e) {
                                        throw new RuntimeException(e);
                                    }
                                    saveEvent(savedPayment, eventPayload);
                                    return CompletableFuture.completedFuture(payment);
                                });
                    }
                });
    }

    private void saveEvent(Payment savedPayment, String payload) {
        OutboxEvent outboxEvent = new OutboxEvent(
                UUID.randomUUID(),
                "Payment",
                savedPayment.getId(),
                "PaymentInitiatedEvent",
                payload,
                Instant.now(),
                false
                );
        outboxEventRepository.save(outboxEvent);
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

    public CompletableFuture<Void> handleSenderDebited(SenderDebitedEvent senderDebitedEvent) {
        log.info("Received SenderDebitedEvent for paymentId: {}", senderDebitedEvent.getPaymentId());
        return paymentRepository.updateStatus(senderDebitedEvent.getPaymentId(), PaymentStatus.SENDER_DEBITED)
                .thenAccept(updatedPaymentOpt -> {
                    if (updatedPaymentOpt.isPresent()) {
                        Payment payment = updatedPaymentOpt.get();
                        ReceiverCreditRequestEvent event = mapPaymentToReceiverCreditRequestEvent(payment);
                        String eventPayload = null;
                        try {
                            eventPayload = objectMapper.writeValueAsString(event);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                        saveEvent(payment, eventPayload);
                        log.info("Payment {} status updated to SENDER_DEBITED. Next: Credit Receiver.", senderDebitedEvent.getPaymentId());
                    } else {
                        log.error("Failed to update payment status to SENDER_DEBITED for paymentId: {}. Payment not found.", senderDebitedEvent.getPaymentId());
                    }
                });
    }

    private static ReceiverCreditRequestEvent mapPaymentToReceiverCreditRequestEvent(Payment payment) {
        ReceiverCreditRequestEvent receiverCreditRequestEvent = new ReceiverCreditRequestEvent();
        receiverCreditRequestEvent.setAccountId(payment.getReceiverAccountId());
        receiverCreditRequestEvent.setCurrency(payment.getCurrency());
        receiverCreditRequestEvent.setPaymentId(payment.getId());
        receiverCreditRequestEvent.setCreditedAmount(payment.getAmount());
        return receiverCreditRequestEvent;
    }

    @Transactional
    public CompletableFuture<Void> handleReceiverCredited(ReceiverCreditEvent event) {
        log.info("Received ReceiverCreditedEvent for paymentId: {}", event.getPaymentId());
        return paymentRepository.updateStatus(event.getPaymentId(), PaymentStatus.COMPLETED)
                .thenCompose(updatedPaymentOpt -> {
                    if (updatedPaymentOpt.isPresent()) {
                        Payment payment = updatedPaymentOpt.get();
                        log.info("Payment {} status updated to COMPLETED. Publishing PaymentCompletedEvent.", event.getPaymentId());
                        return sendPaymentCompletedEvent(payment);
                    } else {
                        log.error("Failed to update payment status to COMPLETED for paymentId: {}. Payment not found.", event.getPaymentId());
                        return CompletableFuture.completedFuture(null);
                    }
                });
    }

    @Transactional
    public CompletableFuture<Void> handleCompensatePayment(CompensatePaymentEvent event) {
        log.info("Received CompensatePaymentEvent for paymentId: {}", event.getPaymentId());
        return paymentRepository.updateStatus(event.getPaymentId(), PaymentStatus.COMPLETED)
                .thenCompose(updatedPaymentOpt -> {
                    if (updatedPaymentOpt.isPresent()) {
                        Payment payment = updatedPaymentOpt.get();
                        log.info("Payment {} status updated to COMPLETED. Publishing PaymentCompletedEvent.", event.getPaymentId());
                        return sendPaymentCompletedEvent(payment);
                    } else {
                        log.error("Failed to update payment status to COMPLETED for paymentId: {}. Payment not found.", event.getPaymentId());
                        return CompletableFuture.completedFuture(null);
                    }
                });
    }

    @Transactional
    public CompletableFuture<Void> handleDebitFailed(DebitFailedEvent event) {
        log.info("Received DebitFailedEvent for paymentId: {}", event.getPaymentId());
        return paymentRepository.updateStatus(event.getPaymentId(), PaymentStatus.DEBIT_FAILED)
                .thenCompose(updatedPaymentOpt -> {
                    if (updatedPaymentOpt.isPresent()) {
                        log.info("Payment {} status updated to DEBIT_FAILED. Publishing PaymentCompletedEvent.", event.getPaymentId());
                    } else {
                        log.error("Failed to update payment status to DEBIT_FAILED for paymentId: {}. Payment not found.", event.getPaymentId());
                    }
                    return CompletableFuture.completedFuture(null);
                });
    }

    @Transactional
    public CompletableFuture<Void> handleCreditFailed(CreditFailedEvent event) {
        log.info("Received DebitFailedEvent for paymentId: {}", event.getPaymentId());
        return paymentRepository.updateStatus(event.getPaymentId(), PaymentStatus.CREDIT_FAILED)
                .thenCompose(updatedPaymentOpt -> {
                    if (updatedPaymentOpt.isPresent()) {
                        Payment payment = updatedPaymentOpt.get();
                        log.info("Payment {} status updated to CREDIT_FAILED. Publishing PaymentCompletedEvent.", event.getPaymentId());
                        return sendCompensatePaymentEventForCreditFailed(payment);
                    } else {
                        log.error("Failed to update payment status to CREDIT_FAILED for paymentId: {}. Payment not found.", event.getPaymentId());
                        return CompletableFuture.completedFuture(null);
                    }
                });
    }

    private CompletableFuture<Void> sendPaymentCompletedEvent(Payment payment) {
        PaymentCompletedEvent event = mapPaymentToPaymentCompletedEvent(payment);
        return paymentProducer.sendPaymentCompletedEvent(event)
                .thenAccept(sendResult -> log.info("PaymentCompletedEvent published for paymentId: {}", payment.getId()))
                .exceptionally(ex -> {
                    log.error("Failed to publish PaymentCompletedEvent for paymentId {}: {}", payment.getId(), ex.getMessage(), ex);
                    // Handle failure to publish completion event (e.g., retry, alert)
                    return null;
                });
    }

    private CompletableFuture<Void> sendCompensatePaymentEventForCreditFailed(Payment payment) {
        CompensatePaymentEvent event = mapPaymentToCompensatePaymentEvent(payment);
        return paymentProducer.sendCompensatePaymentEvent(event)
                .thenAccept(sendResult -> {
                    log.info("CompensatePaymentEvent published for id: {}", payment.getId());
                })
                .exceptionally(ex -> {
                    log.error("Failed to publish CompensatePaymentEvent for paymentId {}: {}", payment.getId(), ex.getMessage(), ex);
                    // Handle failure to publish completion event (e.g., retry, alert)
                    return null;
                });
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
        payment.setAmount(payment.getAmount());
        event.setTimestamp(Instant.now());
        return event;
    }

}