package com.bank.system.payment_service.kafka;

import com.bank.system.payment_service.dto.CreditFailedEvent;
import com.bank.system.payment_service.dto.DebitFailedEvent;
import com.bank.system.payment_service.dto.ReceiverCreditedEvent;
import com.bank.system.payment_service.dto.SenderDebitedEvent;
import com.bank.system.payment_service.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.bank.system.payment_service.kafka.PaymentProducer.CREDIT_FAILED_TOPIC;

@Component
public class PaymentConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentConsumer.class);
    public static final String SENDER_DEBITED_TOPIC = "sender-debited-topic";
    public static final String SPRING_KAFKA_CONSUMER_GROUP_ID = "spring.kafka.consumer.group-id";
    public static final String DEBIT_FAILED_TOPIC = "debit-failed-topic";
    private static final String RECEIVER_CREDITED_TOPIC = "receiver-credited-topic";
    private final PaymentService paymentService;

    public PaymentConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Listens for SenderDebitedEvent from the Account Service.
     *
     * @param event The SenderDebitedEvent received from Kafka.
     */
    @KafkaListener(topics = SENDER_DEBITED_TOPIC, groupId = "${" + SPRING_KAFKA_CONSUMER_GROUP_ID + "}")
    public void listenSenderDebited(SenderDebitedEvent event) {
        log.info("Consumed SenderDebitedEvent for paymentId: {}", event.getPaymentId());
        paymentService.handleSenderDebited(event)
                .exceptionally(ex -> {
                    log.error("Error handling SenderDebitedEvent for paymentId {}: {}", event.getPaymentId(), ex.getMessage(), ex);
                    // Consider sending to a DLQ or implementing retry logic here
                    return null;
                });
    }

    /**
     * Listens for DebitFailedEvent from the Account Service.
     *
     * @param event The DebitFailedEvent received from Kafka.
     */
    @KafkaListener(topics = DEBIT_FAILED_TOPIC, groupId = "${" + SPRING_KAFKA_CONSUMER_GROUP_ID + "}")
    public void listenDebitFailed(DebitFailedEvent event) {
        log.info("Consumed DebitFailedEvent for paymentId: {}", event.getPaymentId());
        paymentService.handleDebitFailed(event)
                .exceptionally(ex -> {
                    log.error("Error handling DebitFailedEvent for paymentId {}: {}", event.getPaymentId(), ex.getMessage(), ex);
                    // Consider sending to a DLQ or implementing retry logic here
                    return null;
                });
    }

    // TODO: Add listeners for ReceiverCreditedEvent, CreditFailedEvent, etc.

    /**
     * Listens for ReceiverCreditedEvent from the Account Service.
     * This event indicates that the receiver's account has been successfully credited.
     * This typically leads to the completion of the payment.
     *
     * @param event The ReceiverCreditedEvent received from Kafka.
     */
    @KafkaListener(topics = RECEIVER_CREDITED_TOPIC, groupId = "${" + SPRING_KAFKA_CONSUMER_GROUP_ID + "}")
    public void listenReceiverCredited(ReceiverCreditedEvent event) {
        log.info("Consumed ReceiverCreditedEvent for paymentId: {}", event.getPaymentId());
        paymentService.handleReceiverCredited(event) // Assuming PaymentService has this method
                .exceptionally(ex -> {
                    log.error("Error handling ReceiverCreditedEvent for paymentId {}: {}", event.getPaymentId(), ex.getMessage(), ex);
                    // Implement DLQ or retry logic here
                    return null;
                });
    }

    /**
     * Listens for CreditFailedEvent from the Account Service.
     * This event indicates that the attempt to credit the receiver's account has failed.
     * This typically leads to the payment failing after a successful debit.
     *
     * @param event The CreditFailedEvent received from Kafka.
     */
    @KafkaListener(topics = CREDIT_FAILED_TOPIC, groupId = "${" + SPRING_KAFKA_CONSUMER_GROUP_ID + "}")
    public void listenCreditFailed(CreditFailedEvent event) {
        log.info("Consumed CreditFailedEvent for paymentId: {}", event.getPaymentId());
        paymentService.handleCreditFailed(event) // Assuming PaymentService has this method
                .exceptionally(ex -> {
                    log.error("Error handling CreditFailedEvent for paymentId {}: {}", event.getPaymentId(), ex.getMessage(), ex);
                    // Implement DLQ or retry logic here
                    return null;
                });
    }
}
