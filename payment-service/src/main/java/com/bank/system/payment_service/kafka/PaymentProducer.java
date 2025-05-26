package com.bank.system.payment_service.kafka;

import com.bank.system.payment_service.dto.CreditFailedEvent;
import com.bank.system.payment_service.dto.PaymentCompletedEvent;
import com.bank.system.payment_service.dto.PaymentFailedEvent;
import com.bank.system.payment_service.dto.PaymentInitiatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture; // Keep this import

@Component
public class PaymentProducer {

    public static final String CREDIT_FAILED_TOPIC = "creadit-failed-topic";
    private static final Logger log = LoggerFactory.getLogger(PaymentProducer.class);
    private static final String PAYMENT_INITIATED_TOPIC = "payment-initiated-topic";
    private static final String PAYMENT_COMPLETED_TOPIC = "payment-completed-topic";
    public static final String PAYMENT_FAILED_TOPIC = "payment-failed-topic";


    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends a PaymentInitiatedEvent to Kafka.
     *
     * @param event The PaymentInitiatedEvent to send.
     * @return A CompletableFuture that completes with the SendResult.
     */
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

    /**
     * Sends a CreditFailedEvent to Kafka.
     *
     * @param event The CreditFailedEvent to send.
     * @return A CompletableFuture that completes with the SendResult.
     */
    public CompletableFuture<SendResult<String, Object>> sendCreditFailedEvent(CreditFailedEvent event) {
        log.info("Sending CreditFailedEvent for paymentId: {}", event.getPaymentId());
        // Directly use the CompletableFuture returned by kafkaTemplate.send()
        return kafkaTemplate.send(CREDIT_FAILED_TOPIC, event.getPaymentId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("CreditFailedEvent sent successfully for paymentId: {} to topic {} with offset {}",
                                event.getPaymentId(), result.getRecordMetadata().topic(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send CreditFailedEvent for paymentId: {}. Reason: {}",
                                event.getPaymentId(), ex.getMessage(), ex);
                    }
                });
    }

    /**
     * Sends a PaymentCompletedEvent to Kafka.
     *
     * @param event The PaymentCompletedEvent to send.
     * @return A CompletableFuture that completes with the SendResult.
     */
    public CompletableFuture<SendResult<String, Object>> sendPaymentCompletedEvent(PaymentCompletedEvent event) {
        log.info("Sending PaymentCompletedEvent for paymentId: {}", event.getPaymentId());
        // Directly use the CompletableFuture returned by kafkaTemplate.send()
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

    /**
     * Sends a PaymentFailedEvent to Kafka.
     *
     * @param event The PaymentFailedEvent to send.
     * @return A CompletableFuture that completes with the SendResult.
     */
    public CompletableFuture<SendResult<String, Object>> sendPaymentFailedEvent(PaymentFailedEvent event) {
        log.info("Sending PaymentFailedEvent for paymentId: {}", event.getPaymentId());
        // Directly use the CompletableFuture returned by kafkaTemplate.send()
        return kafkaTemplate.send(PAYMENT_FAILED_TOPIC, event.getPaymentId(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("PaymentFailedEvent sent successfully for paymentId: {} to topic {} with offset {}",
                                event.getPaymentId(), result.getRecordMetadata().topic(), result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send PaymentFailedEvent for paymentId: {}. Reason: {}",
                                event.getPaymentId(), ex.getMessage(), ex);
                    }
                });
    }
}