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
        return paymentRepository.findByPaymentId(requestDTO.getIdempotencyKey())
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
        payment.setPaymentId(requestDTO.getIdempotencyKey());
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
        event.setTimestamp(Instant.now());
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
                .thenAccept(sendResult -> log.info("PaymentCompletedEvent published for paymentId: {}", payment.getPaymentId()))
                .exceptionally(ex -> {
                    log.error("Failed to publish PaymentCompletedEvent for paymentId {}: {}", payment.getPaymentId(), ex.getMessage(), ex);
                    // Handle failure to publish completion event (e.g., retry, alert)
                    return null;
                });
    }

    private CompletableFuture<Void> sendCompensatePaymentEventForCreditFailed(Payment payment) {
        CompensatePaymentEvent event = mapPaymentToCompensatePaymentEvent(payment);
        return paymentProducer.sendCompensatePaymentEvent(event)
                .thenAccept(sendResult -> {
                    log.info("CompensatePaymentEvent published for paymentId: {}", payment.getPaymentId());
                })
                .exceptionally(ex -> {
                    log.error("Failed to publish CompensatePaymentEvent for paymentId {}: {}", payment.getPaymentId(), ex.getMessage(), ex);
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

    private static CompensatePaymentEvent mapPaymentToCompensatePaymentEvent(Payment payment) {
        CompensatePaymentEvent event = new CompensatePaymentEvent();
        event.setPaymentId(payment.getPaymentId());
        event.setAccountId(payment.getSenderAccountId());
        payment.setAmount(payment.getAmount());
        event.setTimestamp(Instant.now());
        return event;
    }

}