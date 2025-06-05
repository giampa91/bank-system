package com.bank.system.payment_service.kafka;

import com.bank.system.dtos.dto.*;
import com.bank.system.payment_service.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentConsumer.class);
    public static final String SPRING_KAFKA_CONSUMER_GROUP_ID = "spring.kafka.consumer.group-id";

    public static final String SENDER_DEBITED_TOPIC = "sender-debited-topic";
    private static final String RECEIVER_CREDITED_TOPIC = "receiver-credited-topic";
    public static final String DEBIT_FAILED_TOPIC = "debit-failed-topic";
    public static final String CREDIT_FAILED_TOPIC = "credit-failed-topic";
    public static final String COMPENSATE_PAYMENT_TOPIC = "compensate-payment-topic";


    private final PaymentService paymentService;

    @Autowired
    public PaymentConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

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

    @KafkaListener(topics = RECEIVER_CREDITED_TOPIC, groupId = "${" + SPRING_KAFKA_CONSUMER_GROUP_ID + "}")
    public void listenReceiverCredited(ReceiverCreditEvent event) {
        log.info("Consumed ReceiverCreditedEvent for paymentId: {}", event.getPaymentId());
        paymentService.handleReceiverCredited(event) // Assuming PaymentService has this method
                .exceptionally(ex -> {
                    log.error("Error handling ReceiverCreditedEvent for paymentId {}: {}", event.getPaymentId(), ex.getMessage(), ex);
                    // Implement DLQ or retry logic here
                    return null;
                });
    }

    @KafkaListener(topics = DEBIT_FAILED_TOPIC, groupId = "${" + SPRING_KAFKA_CONSUMER_GROUP_ID + "}")
    public void listenDebitFailed(DebitFailedEvent event) {
        log.info("Consumed DebitFailedEvent for paymentId: {}", event.getPaymentId());
        paymentService.handleDebitFailed(event) // Assuming PaymentService has this method
                .exceptionally(ex -> {
                    log.error("Error handling DebitFailedEvent for paymentId {}: {}", event.getPaymentId(), ex.getMessage(), ex);
                    // Implement DLQ or retry logic here
                    return null;
                });
    }

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

    @KafkaListener(topics = COMPENSATE_PAYMENT_TOPIC, groupId = "${" + SPRING_KAFKA_CONSUMER_GROUP_ID + "}")
    public void listenCompensatePayment(CompensatePaymentEvent event) {
        log.info("Consumed ReceiverCreditedEvent for paymentId: {}", event.getPaymentId());
        paymentService.handleCompensatePayment(event) // Assuming PaymentService has this method
                .exceptionally(ex -> {
                    log.error("Error handling ReceiverCreditedEvent for paymentId {}: {}", event.getPaymentId(), ex.getMessage(), ex);
                    // Implement DLQ or retry logic here
                    return null;
                });
    }

}
