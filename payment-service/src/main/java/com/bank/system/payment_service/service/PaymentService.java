package com.bank.system.payment_service.service;

import com.bank.system.payment_service.domain.Payment;
import com.bank.system.payment_service.domain.PaymentStatus;
import com.bank.system.payment_service.dto.*;
import com.bank.system.payment_service.kafka.PaymentProducer;
import com.bank.system.payment_service.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentProducer paymentProducer;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentProducer paymentProducer) {
        this.paymentRepository = paymentRepository;
        this.paymentProducer = paymentProducer;
    }

    /**
     * Initiates a new payment.
     * This is the first step of the payment saga.
     *
     * @param requestDTO The payment request details.
     * @return A CompletableFuture that completes with the initiated Payment object.
     */
    @Transactional // Ensures atomicity for local DB operations (saving payment)
    public CompletableFuture<Payment> initiatePayment(PaymentRequestDTO requestDTO) {
        // 1. Check for idempotency key to prevent duplicate requests
        return paymentRepository.findByPaymentId(requestDTO.getIdempotencyKey()) // Assuming idempotencyKey is used as paymentId for lookup
                .thenCompose(existingPaymentOpt -> {
                    if (existingPaymentOpt.isPresent()) {
                        Payment existingPayment = existingPaymentOpt.get();
                        log.warn("Payment with idempotencyKey {} already exists with status: {}",
                                requestDTO.getIdempotencyKey(), existingPayment.getStatus());
                        // Return existing payment if already processed or in progress
                        return CompletableFuture.completedFuture(existingPayment);
                    } else {
                        // 2. Manually map DTO to Entity and set initial status
                        Payment payment = new Payment();
                        // payment.setId(UUID.randomUUID()); // ID will be set by repository.save()
                        payment.setPaymentId(requestDTO.getIdempotencyKey()); // Use idempotency key as paymentId for simplicity
                        payment.setSenderAccountId(requestDTO.getSenderAccountId());
                        payment.setReceiverAccountId(requestDTO.getReceiverAccountId());
                        payment.setAmount(requestDTO.getAmount());
                        payment.setCurrency(requestDTO.getCurrency());
                        payment.setIdempotencyKey(requestDTO.getIdempotencyKey());
                        payment.setStatus(PaymentStatus.INITIATED);
                        // createdAt and updatedAt will be set by repository.save()

                        // 3. Save payment to local database
                        return paymentRepository.save(payment)
                                .thenCompose(savedPayment -> {
                                    // 4. Manually map saved Payment to PaymentInitiatedEvent
                                    PaymentInitiatedEvent event = new PaymentInitiatedEvent();
                                    event.setPaymentId(savedPayment.getPaymentId());
                                    event.setSenderAccountId(savedPayment.getSenderAccountId());
                                    event.setReceiverAccountId(savedPayment.getReceiverAccountId());
                                    event.setAmount(savedPayment.getAmount());
                                    event.setCurrency(savedPayment.getCurrency());
                                    event.setIdempotencyKey(savedPayment.getIdempotencyKey());
                                    event.setTimestamp(Instant.now()); // Set event timestamp

                                    return paymentProducer.sendPaymentInitiatedEvent(event)
                                            .thenApply(sendResult -> {
                                                log.info("Payment {} initiated and event published.", savedPayment.getPaymentId());
                                                return savedPayment;
                                            });
                                });
                    }
                });
    }

    /**
     * Handles the SenderDebitedEvent from Account Service.
     * This is a step in the payment saga.
     *
     * @param event The SenderDebitedEvent.
     * @return A CompletableFuture that completes when processing is done.
     */
    public CompletableFuture<Void> handleSenderDebited(SenderDebitedEvent event) {
        log.info("Received SenderDebitedEvent for paymentId: {}", event.getPaymentId());
        return paymentRepository.updateStatus(event.getPaymentId(), PaymentStatus.SENDER_DEBITED)
                .thenAccept(updatedPaymentOpt -> {
                    if (updatedPaymentOpt.isPresent()) {
                        // TODO: Publish CreditReceiverEvent to Kafka for the receiver account
                        log.info("Payment {} status updated to SENDER_DEBITED. Next: Credit Receiver.", event.getPaymentId());
                    } else {
                        log.error("Failed to update payment status to SENDER_DEBITED for paymentId: {}. Payment not found.", event.getPaymentId());
                    }
                });
    }

    /**
     * Handles the DebitFailedEvent from Account Service.
     * This is a compensating step in the payment saga.
     *
     * @param event The DebitFailedEvent.
     * @return A CompletableFuture that completes when processing is done.
     */
    public CompletableFuture<Void> handleDebitFailed(DebitFailedEvent event) {
        log.warn("Received DebitFailedEvent for paymentId: {}. Reason: {}", event.getPaymentId(), event.getReason());
        return paymentRepository.updateStatus(event.getPaymentId(), PaymentStatus.FAILED)
                .thenAccept(updatedPaymentOpt -> {
                    if (updatedPaymentOpt.isPresent()) {
                        log.info("Payment {} status updated to FAILED due to debit failure.", event.getPaymentId());
                        // TODO: Potentially publish a PaymentFailedEvent for other services/notifications
                    } else {
                        log.error("Failed to update payment status to FAILED for paymentId: {}. Payment not found.", event.getPaymentId());
                    }
                });
    }

    /**
     * Handles the CreditFailedEvent from Account Service.
     * This indicates that crediting the receiver's account failed,
     * requiring a compensation (e.g., refunding the sender).
     *
     * @param event The CreditFailedEvent.
     * @return A CompletableFuture that completes when processing is done.
     */
    @Transactional // Ensures atomicity for local DB operations
    public CompletableFuture<Void> handleCreditFailed(CreditFailedEvent event) {
        log.warn("Received CreditFailedEvent for paymentId: {}. Reason: {}", event.getPaymentId(), event.getReason());
        return paymentRepository.updateStatus(event.getPaymentId(), PaymentStatus.CREDIT_FAILED) // Set a specific status for credit failure
                .thenAccept(updatedPaymentOpt -> {
                    if (updatedPaymentOpt.isPresent()) {
                        log.info("Payment {} status updated to CREDIT_FAILED due to credit failure.", event.getPaymentId());
                        // TODO: Implement compensation logic here, e.g., send an event to refund the sender.
                        // You might also publish a PaymentFailedEvent here for other services.
                    } else {
                        log.error("Failed to update payment status to CREDIT_FAILED for paymentId: {}. Payment not found.", event.getPaymentId());
                    }
                });
    }

    /**
     * Publishes a PaymentCompletedEvent to Kafka.
     * This method is called once the entire payment saga is successfully completed.
     *
     * @param payment The completed Payment entity.
     * @return A CompletableFuture that completes when the event is published.
     */
    private CompletableFuture<Void> sendPaymentCompletedEvent(Payment payment) {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setPaymentId(payment.getPaymentId());
        event.setSenderAccountId(payment.getSenderAccountId());
        event.setReceiverAccountId(payment.getReceiverAccountId());
        event.setAmount(payment.getAmount());
        event.setCurrency(payment.getCurrency());
        event.setTimestamp(Instant.now());

        return paymentProducer.sendPaymentCompletedEvent(event) // Assuming this method exists in PaymentProducer
                .thenAccept(sendResult -> log.info("PaymentCompletedEvent published for paymentId: {}", payment.getPaymentId()))
                .exceptionally(ex -> {
                    log.error("Failed to publish PaymentCompletedEvent for paymentId {}: {}", payment.getPaymentId(), ex.getMessage(), ex);
                    // Handle failure to publish completion event (e.g., retry, alert)
                    return null;
                });
    }/**
     * Handles the ReceiverCreditedEvent from Account Service.
     * This indicates the receiver's account has been successfully credited,
     * marking the completion of the core payment transaction.
     *
     * @param event The ReceiverCreditedEvent.
     * @return A CompletableFuture that completes when processing is done.
     */
    @Transactional // Ensures atomicity for local DB operations
    public CompletableFuture<Void> handleReceiverCredited(ReceiverCreditedEvent event) {
        log.info("Received ReceiverCreditedEvent for paymentId: {}", event.getPaymentId());
        return paymentRepository.updateStatus(event.getPaymentId(), PaymentStatus.COMPLETED)
                .thenCompose(updatedPaymentOpt -> {
                    if (updatedPaymentOpt.isPresent()) {
                        Payment payment = updatedPaymentOpt.get();
                        log.info("Payment {} status updated to COMPLETED. Publishing PaymentCompletedEvent.", event.getPaymentId());
                        return sendPaymentCompletedEvent(payment); // Publish the final completion event
                    } else {
                        log.error("Failed to update payment status to COMPLETED for paymentId: {}. Payment not found.", event.getPaymentId());
                        return CompletableFuture.completedFuture(null);
                    }
                });
    }

}

