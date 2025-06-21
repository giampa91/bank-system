package com.bank.system.payment_service.mapper;

import com.bank.system.dtos.dto.*;
import com.bank.system.payment_service.domain.Payment;
import com.bank.system.payment_service.domain.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public class PaymentMapper {

    public static PaymentCompletedEvent mapPaymentToPaymentCompletedEvent(Payment payment) {
        PaymentCompletedEvent event = new PaymentCompletedEvent();
        event.setEventId(UUID.randomUUID());
        event.setPaymentId(payment.getId());
        event.setSenderAccountId(payment.getSenderAccountId());
        event.setReceiverAccountId(payment.getReceiverAccountId());
        event.setAmount(payment.getAmount());
        event.setCurrency(payment.getCurrency());
        event.setTimestamp(Instant.now());
        return event;
    }

    public static CompensatePaymentEvent mapPaymentToCompensatePaymentEvent(Payment payment) {
        CompensatePaymentEvent event = new CompensatePaymentEvent();
        event.setEventId(UUID.randomUUID());
        event.setPaymentId(payment.getId());
        event.setAccountId(payment.getSenderAccountId());
        event.setAmount(payment.getAmount());
        event.setTimestamp(Instant.now());
        return event;
    }

    public static Payment mapPaymentRequestDtoToPayment(PaymentRequestDTO requestDTO) {
        Payment payment = new Payment();
        payment.setSenderAccountId(requestDTO.getSenderAccountId());
        payment.setReceiverAccountId(requestDTO.getReceiverAccountId());
        payment.setAmount(requestDTO.getAmount());
        payment.setCurrency(requestDTO.getCurrency());
        payment.setIdempotencyKey(requestDTO.getIdempotencyKey());
        payment.setStatus(PaymentStatus.INITIATED);
        return payment;
    }

    public static PaymentInitiatedEvent mapToPaymentInitiatedEvent(Payment savedPayment) {
        PaymentInitiatedEvent event = new PaymentInitiatedEvent();
        event.setEventId(UUID.randomUUID());
        event.setPaymentId(savedPayment.getId());
        event.setSenderAccountId(savedPayment.getSenderAccountId());
        event.setReceiverAccountId(savedPayment.getReceiverAccountId());
        event.setAmount(savedPayment.getAmount());
        event.setCurrency(savedPayment.getCurrency());
        event.setIdempotencyKey(savedPayment.getIdempotencyKey());
        event.setTimestamp(Instant.now());
        return event;
    }

    public static ReceiverCreditRequestEvent mapPaymentToReceiverCreditRequestEvent(Payment payment) {
        ReceiverCreditRequestEvent event = new ReceiverCreditRequestEvent();
        event.setEventId(UUID.randomUUID());
        event.setAccountId(payment.getReceiverAccountId());
        event.setCurrency(payment.getCurrency());
        event.setPaymentId(payment.getId());
        event.setCreditedAmount(payment.getAmount());
        return event;
    }
}