package com.bank.system.account_service.kafka;

import com.bank.system.dtos.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class AccountProducer {


    private static final Logger log = LoggerFactory.getLogger(AccountProducer.class);

    public static final String SENDER_DEBITED_TOPIC = "sender-debited-topic";
    public static final String RECEIVER_CREDIT_TOPIC = "receiver-credit-topic";
    public static final String DEBIT_FAILED_TOPIC = "debit-failed-topic";
    public static final String CREDIT_FAILED_TOPIC = "credit-failed-topic";
    public static final String COMPENSATE_PAYMENT = "compensate-payment-topic";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public AccountProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    public CompletableFuture<SendResult<String, Object>> sendSenderDebitedEvent(SenderDebitedEvent event) {
        log.info("Sending SenderDebitedEvent for paymentId: {}", event.getPaymentId());
        return kafkaTemplate.send(SENDER_DEBITED_TOPIC, event.getPaymentId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("SenderDebitedEvent sent successfully for paymentId: {} to topic {} with offset {}",
                                event.getPaymentId(), result.getRecordMetadata().topic(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send SenderDebitedEvent for paymentId: {}. Reason: {}",
                                event.getPaymentId(), ex.getMessage(), ex);
                    }
                });
    }

    public CompletableFuture<SendResult<String, Object>> sendReceiverCreditEvent(ReceiverCreditEvent event) {
        return kafkaTemplate.send(RECEIVER_CREDIT_TOPIC, event.getPaymentId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("ReceiverCreditEvent sent successfully for paymentId: {} to topic {} with offset {}",
                                event.getPaymentId(), result.getRecordMetadata().topic(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send ReceiverCreditEvent for paymentId: {}. Reason: {}",
                                event.getPaymentId(), ex.getMessage(), ex);
                    }
                });
    }

    public CompletableFuture<SendResult<String, Object>> sendCompensatePaymentEvent(CompensatePaymentEvent event) {
        return kafkaTemplate.send(COMPENSATE_PAYMENT, event.getPaymentId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("CompensatePaymentRequestEvent sent successfully for paymentId: {} to topic {} with offset {}",
                                event.getPaymentId(), result.getRecordMetadata().topic(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send CompensatePaymentRequestEvent for paymentId: {}. Reason: {}",
                                event.getPaymentId(), ex.getMessage(), ex);
                    }
                });
    }

    public CompletableFuture<SendResult<String, Object>> sendSenderDebitedFailedEvent(DebitFailedEvent event) {
        log.info("Sending DebitFailedEvent for paymentId: {}", event.getPaymentId());
        return kafkaTemplate.send(DEBIT_FAILED_TOPIC, event.getPaymentId(), event);
    }

    public CompletableFuture<SendResult<String, Object>> sendSenderCreditedFailedEvent(CreditFailedEvent event) {
        log.info("Sending CreditFailedEvent for paymentId: {}", event.getPaymentId());
        return kafkaTemplate.send(CREDIT_FAILED_TOPIC, event.getPaymentId(), event);
    }

}