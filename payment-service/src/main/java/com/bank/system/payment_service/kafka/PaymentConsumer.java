package com.bank.system.payment_service.kafka;

import com.bank.system.dtos.dto.PaymentInitiatedEvent;
import com.bank.system.dtos.dto.ReceiverCreditEvent;
import com.bank.system.dtos.dto.SenderDebitedEvent;
import com.bank.system.payment_service.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentConsumer.class);
    public static final String SENDER_DEBITED_TOPIC = "sender-debited-topic";
    public static final String SPRING_KAFKA_CONSUMER_GROUP_ID = "spring.kafka.consumer.group-id";
    private static final String RECEIVER_CREDITED_TOPIC = "receiver-credited-topic";
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

}
