package com.bank.system.payment_service.kafka;

import com.bank.system.dtos.dto.PaymentCompletedEvent;
import com.bank.system.dtos.dto.PaymentInitiatedEvent;
import com.bank.system.dtos.dto.ReceiverCreditRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class PaymentProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentProducer.class);

    private static final String PAYMENT_COMPLETED_TOPIC = "payment-completed-topic";

    private static final String PAYMENT_INITIATED_TOPIC = "payment-initiated-topic";

    private static final String RECEIVER_CREDITED_REQUESTED_TOPIC = "receiver-credited-requested-topic";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public PaymentProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<SendResult<String, Object>> sendPaymentInitiatedEvent(PaymentInitiatedEvent event) {
        log.info("Sending PaymentInitiatedEvent for paymentId: {}", event.getPaymentId());
        return kafkaTemplate.send(PAYMENT_INITIATED_TOPIC, event.getPaymentId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("PaymentInitiatedEvent sent successfully for paymentId: {} to topic {} with offset {}",
                                event.getPaymentId(), result.getRecordMetadata().topic(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send PaymentInitiatedEvent for paymentId: {}. Reason: {}",
                                event.getPaymentId(), ex.getMessage(), ex);
                    }
                });
    }

    public CompletableFuture<SendResult<String, Object>> sendReceiverCreditRequestEvent(ReceiverCreditRequestEvent event) {
        log.info("Sending ReceiverCreditRequestEvent for paymentId: {}", event.getPaymentId());
        return kafkaTemplate.send(RECEIVER_CREDITED_REQUESTED_TOPIC, event.getPaymentId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("ReceiverCreditRequestEvent sent successfully for paymentId: {} to topic {} with offset {}",
                                event.getPaymentId(), result.getRecordMetadata().topic(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send ReceiverCreditRequestEvent for paymentId: {}. Reason: {}",
                                event.getPaymentId(), ex.getMessage(), ex);
                    }
                });
    }

    public CompletableFuture<SendResult<String, Object>> sendPaymentCompletedEvent(PaymentCompletedEvent event) {
        log.info("Sending PaymentCompletedEvent for paymentId: {}", event.getPaymentId());
        return kafkaTemplate.send(PAYMENT_COMPLETED_TOPIC, event.getPaymentId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("PaymentCompletedEvent sent successfully for paymentId: {} to topic {} with offset {}",
                                event.getPaymentId(), result.getRecordMetadata().topic(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send PaymentCompletedEvent for paymentId: {}. Reason: {}",
                                event.getPaymentId(), ex.getMessage(), ex);
                    }
                });
    }

}