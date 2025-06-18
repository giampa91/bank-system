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
import java.util.concurrent.CompletableFuture; // Still needed for .join() if repository remains async

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

    /**
     * Initiates a payment. If a payment with the given idempotency key already exists,
     * it returns the existing payment. Otherwise, it creates a new payment, saves it,
     * and publishes a PaymentInitiatedEvent.
     * This method is now synchronous, blocking until repository operations complete.
     *
     * @param requestDTO The payment request data transfer object.
     * @return The initiated or existing Payment object.
     * @throws RuntimeException if JSON processing fails.
     */
    @Transactional
    public Payment initiatePayment(PaymentRequestDTO requestDTO) {
        // Find existing payment by idempotency key. .join() makes this call synchronous.
        Optional<Payment> existingPaymentOpt = paymentRepository.findByIdempotencyKeyId(requestDTO.getIdempotencyKey());

        if (existingPaymentOpt.isPresent()) {
            Payment existingPayment = existingPaymentOpt.get();
            log.warn("Payment with idempotencyKey {} already exists with status: {}",
                    requestDTO.getIdempotencyKey(), existingPayment.getStatus());
            return existingPayment;
        } else {
            Payment payment = mapPaymentRequestDtoToPayment(requestDTO);
            // Save the new payment. .join() makes this call synchronous.
            Payment savedPayment = paymentRepository.save(payment);

            PaymentInitiatedEvent event = mapToPaymentInitiatedEvent(savedPayment);
            String eventPayload;
            try {
                eventPayload = objectMapper.writeValueAsString(event);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize PaymentInitiatedEvent for paymentId {}: {}", savedPayment.getId(), e.getMessage(), e);
                throw new RuntimeException("Failed to serialize PaymentInitiatedEvent", e);
            }
            // Save the outbox event.
            saveEvent(savedPayment, eventPayload);
            log.info("Payment initiated and event saved for paymentId: {}", savedPayment.getId());
            return savedPayment; // Return the savedPayment
        }
    }

    /**
     * Saves an OutboxEvent to the repository.
     *
     * @param savedPayment The payment associated with the event.
     * @param payload The JSON payload of the event.
     */
    private void saveEvent(Payment savedPayment, String payload) {
        OutboxEvent outboxEvent = new OutboxEvent(
                UUID.randomUUID(),
                "Payment",
                savedPayment.getId(),
                "PaymentInitiatedEvent", // This specific event type should likely be dynamic based on the actual event being saved
                payload,
                Instant.now(),
                false
        );
        // Save the outbox event. .join() makes this call synchronous.
        outboxEventRepository.save(outboxEvent);
        log.debug("Outbox event saved for paymentId: {}", savedPayment.getId());
    }

    /**
     * Maps a PaymentRequestDTO to a Payment entity.
     *
     * @param requestDTO The payment request DTO.
     * @return A new Payment entity.
     */
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

    /**
     * Maps a Payment entity to a PaymentInitiatedEvent DTO.
     *
     * @param savedPayment The saved Payment entity.
     * @return A new PaymentInitiatedEvent DTO.
     */
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

    /**
     * Handles the SenderDebitedEvent, updating the payment status and requesting receiver credit.
     * This method is now synchronous.
     *
     * @param senderDebitedEvent The event indicating the sender has been debited.
     * @throws RuntimeException if JSON processing fails.
     */
    @Transactional
    public void handleSenderDebited(SenderDebitedEvent senderDebitedEvent) {
        log.info("Received SenderDebitedEvent for paymentId: {}", senderDebitedEvent.getPaymentId());
        // Update payment status. .join() makes this call synchronous.
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
            // Save the outbox event.
            saveEvent(payment, eventPayload);
            log.info("Payment {} status updated to SENDER_DEBITED. Next: Credit Receiver.", senderDebitedEvent.getPaymentId());
        } else {
            log.error("Failed to update payment status to SENDER_DEBITED for paymentId: {}. Payment not found.", senderDebitedEvent.getPaymentId());
        }
    }

    /**
     * Maps a Payment entity to a ReceiverCreditRequestEvent DTO.
     *
     * @param payment The Payment entity.
     * @return A new ReceiverCreditRequestEvent DTO.
     */
    private static ReceiverCreditRequestEvent mapPaymentToReceiverCreditRequestEvent(Payment payment) {
        ReceiverCreditRequestEvent receiverCreditRequestEvent = new ReceiverCreditRequestEvent();
        receiverCreditRequestEvent.setAccountId(payment.getReceiverAccountId());
        receiverCreditRequestEvent.setCurrency(payment.getCurrency());
        receiverCreditRequestEvent.setPaymentId(payment.getId());
        receiverCreditRequestEvent.setCreditedAmount(payment.getAmount());
        return receiverCreditRequestEvent;
    }

    /**
     * Handles the ReceiverCreditedEvent, updating the payment status to COMPLETED
     * and publishing a PaymentCompletedEvent.
     * This method is now synchronous.
     *
     * @param event The event indicating the receiver has been credited.
     */
    @Transactional
    public void handleReceiverCredited(ReceiverCreditEvent event) {
        log.info("Received ReceiverCreditedEvent for paymentId: {}", event.getPaymentId());
        // Update payment status. .join() makes this call synchronous.
        Optional<Payment> updatedPaymentOpt = paymentRepository.updateStatus(event.getPaymentId(), PaymentStatus.COMPLETED);

        if (updatedPaymentOpt.isPresent()) {
            Payment payment = updatedPaymentOpt.get();
            log.info("Payment {} status updated to COMPLETED. Publishing PaymentCompletedEvent.", event.getPaymentId());
            sendPaymentCompletedEvent(payment); // Call synchronous helper
        } else {
            log.error("Failed to update payment status to COMPLETED for paymentId: {}. Payment not found.", event.getPaymentId());
        }
    }

    /**
     * Handles the CompensatePaymentEvent, updating the payment status to COMPLETED
     * and publishing a PaymentCompletedEvent.
     * This method is now synchronous.
     *
     * @param event The event indicating a payment compensation.
     */
    @Transactional
    public void handleCompensatePayment(CompensatePaymentEvent event) {
        log.info("Received CompensatePaymentEvent for paymentId: {}", event.getPaymentId());
        // Update payment status. .join() makes this call synchronous.
        Optional<Payment> updatedPaymentOpt = paymentRepository.updateStatus(event.getPaymentId(), PaymentStatus.COMPLETED);

        if (updatedPaymentOpt.isPresent()) {
            Payment payment = updatedPaymentOpt.get();
            log.info("Payment {} status updated to COMPLETED. Publishing PaymentCompletedEvent.", event.getPaymentId());
            sendPaymentCompletedEvent(payment); // Call synchronous helper
        } else {
            log.error("Failed to update payment status to COMPLETED for paymentId: {}. Payment not found.", event.getPaymentId());
        }
    }

    /**
     * Handles the DebitFailedEvent, updating the payment status to DEBIT_FAILED.
     * This method is now synchronous.
     *
     * @param event The event indicating a debit failure.
     */
    @Transactional
    public void handleDebitFailed(DebitFailedEvent event) {
        log.info("Received DebitFailedEvent for paymentId: {}", event.getPaymentId());
        // Update payment status. .join() makes this call synchronous.
        Optional<Payment> updatedPaymentOpt = paymentRepository.updateStatus(event.getPaymentId(), PaymentStatus.DEBIT_FAILED);

        if (updatedPaymentOpt.isPresent()) {
            log.info("Payment {} status updated to DEBIT_FAILED.", event.getPaymentId());
        } else {
            log.error("Failed to update payment status to DEBIT_FAILED for paymentId: {}. Payment not found.", event.getPaymentId());
        }
    }

    /**
     * Handles the CreditFailedEvent, updating the payment status to CREDIT_FAILED
     * and sending a compensation event.
     * This method is now synchronous.
     *
     * @param event The event indicating a credit failure.
     */
    @Transactional
    public void handleCreditFailed(CreditFailedEvent event) {
        log.info("Received CreditFailedEvent for paymentId: {}", event.getPaymentId());
        Optional<Payment> updatedPaymentOpt = paymentRepository.updateStatus(event.getPaymentId(), PaymentStatus.CREDIT_FAILED);

        if (updatedPaymentOpt.isPresent()) {
            Payment payment = updatedPaymentOpt.get();
            log.info("Payment {} status updated to CREDIT_FAILED. Publishing CompensatePaymentEvent.", event.getPaymentId());
            sendCompensatePaymentEventForCreditFailed(payment); // Call synchronous helper
        } else {
            log.error("Failed to update payment status to CREDIT_FAILED for paymentId: {}. Payment not found.", event.getPaymentId());
        }
    }

    /**
     * Sends a PaymentCompletedEvent.
     * This method is now synchronous.
     *
     * @param payment The completed Payment entity.
     */
    private void sendPaymentCompletedEvent(Payment payment) {
        PaymentCompletedEvent event = mapPaymentToPaymentCompletedEvent(payment);
        try {
            // Send event. .join() makes this call synchronous.
            paymentProducer.sendPaymentCompletedEvent(event).join();
            log.info("PaymentCompletedEvent published for paymentId: {}", payment.getId());
        } catch (Exception ex) {
            log.error("Failed to publish PaymentCompletedEvent for paymentId {}: {}", payment.getId(), ex.getMessage(), ex);
            // Re-throw or handle as per application's error handling policy (e.g., dead-letter queue, retry mechanism)
            throw new RuntimeException("Failed to publish PaymentCompletedEvent", ex);
        }
    }

    /**
     * Sends a CompensatePaymentEvent for credit failure.
     * This method is now synchronous.
     *
     * @param payment The payment for which compensation is needed.
     */
    private void sendCompensatePaymentEventForCreditFailed(Payment payment) {
        CompensatePaymentEvent event = mapPaymentToCompensatePaymentEvent(payment);
        try {
            // Send event. .join() makes this call synchronous.
            paymentProducer.sendCompensatePaymentEvent(event).join();
            log.info("CompensatePaymentEvent published for paymentId: {}", payment.getId());
        } catch (Exception ex) {
            log.error("Failed to publish CompensatePaymentEvent for paymentId {}: {}", payment.getId(), ex.getMessage(), ex);
            // Re-throw or handle as per application's error handling policy
            throw new RuntimeException("Failed to publish CompensatePaymentEvent", ex);
        }
    }

    /**
     * Maps a Payment entity to a PaymentCompletedEvent DTO.
     *
     * @param payment The Payment entity.
     * @return A new PaymentCompletedEvent DTO.
     */
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

    /**
     * Maps a Payment entity to a CompensatePaymentEvent DTO.
     *
     * @param payment The Payment entity.
     * @return A new CompensatePaymentEvent DTO.
     */
    private static CompensatePaymentEvent mapPaymentToCompensatePaymentEvent(Payment payment) {
        CompensatePaymentEvent event = new CompensatePaymentEvent();
        event.setPaymentId(payment.getId());
        event.setAccountId(payment.getSenderAccountId());
        event.setAmount(payment.getAmount()); // Corrected: Set amount from payment
        event.setTimestamp(Instant.now());
        return event;
    }
}
