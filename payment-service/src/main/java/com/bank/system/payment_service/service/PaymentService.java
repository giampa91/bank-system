package com.bank.system.payment_service.service;

import com.bank.system.dtos.dto.*;
import com.bank.system.payment_service.domain.Payment;
import com.bank.system.payment_service.domain.PaymentStatus;
import com.bank.system.payment_service.kafka.PaymentProducer;
import com.bank.system.payment_service.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    @Transactional
    public CompletableFuture<Payment> initiatePayment(PaymentRequestDTO requestDTO) {
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
                        Payment payment = mapPaymentRequestDtoToPayment(requestDTO);
                        // createdAt and updatedAt will be set by repository.save()
                        // 3. Save payment to local database
                        return paymentRepository.save(payment)
                                .thenCompose(savedPayment -> {
                                    // 4. Manually map saved Payment to PaymentInitiatedEvent
                                    PaymentInitiatedEvent event = mapToPaymentInitiatedEvent(savedPayment);
                                    return paymentProducer.sendPaymentInitiatedEvent(event)
                                            .thenApply(sendResult -> {
                                                log.info("Payment {} initiated and event published.", savedPayment.getPaymentId());
                                                return savedPayment;
                                            });
                                });
                    }
                });
    }

    private static Payment mapPaymentRequestDtoToPayment(PaymentRequestDTO requestDTO) {
        Payment payment = new Payment();
        // payment.setId(UUID.randomUUID()); // ID will be set by repository.save()
        payment.setPaymentId(requestDTO.getIdempotencyKey()); // Use idempotency key as paymentId for simplicity
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
        event.setPaymentId(savedPayment.getPaymentId());
        event.setSenderAccountId(savedPayment.getSenderAccountId());
        event.setReceiverAccountId(savedPayment.getReceiverAccountId());
        event.setAmount(savedPayment.getAmount());
        event.setCurrency(savedPayment.getCurrency());
        event.setIdempotencyKey(savedPayment.getIdempotencyKey());
        event.setTimestamp(Instant.now()); // Set event timestamp
        return event;
    }

    public CompletableFuture<Void> handleSenderDebited(SenderDebitedEvent senderDebitedEvent) {
        log.info("Received SenderDebitedEvent for paymentId: {}", senderDebitedEvent.getPaymentId());
        return paymentRepository.updateStatus(senderDebitedEvent.getPaymentId(), PaymentStatus.SENDER_DEBITED)
                .thenAccept(updatedPaymentOpt -> {
                    if (updatedPaymentOpt.isPresent()) {
                        ReceiverCreditRequestEvent ReceiverCreditRequestEvent = mapPaymentToReceiverCreditRequestEvent(updatedPaymentOpt.get());
                        paymentProducer.sendReceiverCreditRequestEvent(ReceiverCreditRequestEvent);
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
        receiverCreditRequestEvent.setPaymentId(payment.getPaymentId());
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
                        return sendPaymentCompletedEvent(payment); // Publish the final completion event
                    } else {
                        log.error("Failed to update payment status to COMPLETED for paymentId: {}. Payment not found.", event.getPaymentId());
                        return CompletableFuture.completedFuture(null);
                    }
                });
    }

    private CompletableFuture<Void> sendPaymentCompletedEvent(Payment payment) {
        PaymentCompletedEvent event = mapPaymentToPaymentCompletedEvent(payment);
        return paymentProducer.sendPaymentCompletedEvent(event)
                .thenAccept(sendResult -> log.info("PaymentCompletedEvent published for paymentId: {}", payment.getPaymentId()))
                .exceptionally(ex -> {
                    log.error("Failed to publish PaymentCompletedEvent for paymentId {}: {}", payment.getPaymentId(), ex.getMessage(), ex);
                    // Handle failure to publish completion event (e.g., retry, alert)
                    return null;
                });
    }

    private static PaymentCompletedEvent mapPaymentToPaymentCompletedEvent(Payment payment) {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setPaymentId(payment.getPaymentId());
        event.setSenderAccountId(payment.getSenderAccountId());
        event.setReceiverAccountId(payment.getReceiverAccountId());
        event.setAmount(payment.getAmount());
        event.setCurrency(payment.getCurrency());
        event.setTimestamp(Instant.now());
        return event;
    }

}