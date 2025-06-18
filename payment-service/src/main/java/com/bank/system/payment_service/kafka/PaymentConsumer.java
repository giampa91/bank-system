package com.bank.system.payment_service.kafka;

import com.bank.system.dtos.dto.*;
import com.bank.system.payment_service.service.PaymentAccountService;
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

    private final PaymentAccountService paymentAccountService;

    @Autowired
    public PaymentConsumer(PaymentAccountService paymentAccountService) {
        this.paymentAccountService = paymentAccountService;
    }

    @KafkaListener(topics = SENDER_DEBITED_TOPIC, groupId = "${" + SPRING_KAFKA_CONSUMER_GROUP_ID + "}")
    public void listenSenderDebited(SenderDebitedEvent event) {
        log.info("Consumed SenderDebitedEvent for paymentId: {}", event.getPaymentId());
        try {
            paymentAccountService.handleSenderDebited(event);
        } catch (Exception ex) {
            log.error("Error handling SenderDebitedEvent for paymentId {}: {}", event.getPaymentId(), ex.getMessage(), ex);
            // Consider sending to a DLQ or implementing retry logic here
        }
    }

    @KafkaListener(topics = RECEIVER_CREDITED_TOPIC, groupId = "${" + SPRING_KAFKA_CONSUMER_GROUP_ID + "}")
    public void listenReceiverCredited(ReceiverCreditEvent event) {
        log.info("Consumed ReceiverCreditedEvent for paymentId: {}", event.getPaymentId());
        try {
            paymentAccountService.handleReceiverCredited(event);
        } catch (Exception ex) {
            log.error("Error handling ReceiverCreditedEvent for paymentId {}: {}", event.getPaymentId(), ex.getMessage(), ex);
        }
    }

    @KafkaListener(topics = DEBIT_FAILED_TOPIC, groupId = "${" + SPRING_KAFKA_CONSUMER_GROUP_ID + "}")
    public void listenDebitFailed(DebitFailedEvent event) {
        log.info("Consumed DebitFailedEvent for paymentId: {}", event.getPaymentId());
        try {
            paymentAccountService.handleDebitFailed(event);
        } catch (Exception ex) {
            log.error("Error handling DebitFailedEvent for paymentId {}: {}", event.getPaymentId(), ex.getMessage(), ex);
        }
    }

    @KafkaListener(topics = CREDIT_FAILED_TOPIC, groupId = "${" + SPRING_KAFKA_CONSUMER_GROUP_ID + "}")
    public void listenCreditFailed(CreditFailedEvent event) {
        log.info("Consumed CreditFailedEvent for paymentId: {}", event.getPaymentId());
        try {
            paymentAccountService.handleCreditFailed(event);
        } catch (Exception ex) {
            log.error("Error handling CreditFailedEvent for paymentId {}: {}", event.getPaymentId(), ex.getMessage(), ex);
        }
    }

    @KafkaListener(topics = COMPENSATE_PAYMENT_TOPIC, groupId = "${" + SPRING_KAFKA_CONSUMER_GROUP_ID + "}")
    public void listenCompensatePayment(CompensatePaymentEvent event) {
        log.info("Consumed CompensatePaymentEvent for paymentId: {}", event.getPaymentId());
        try {
            paymentAccountService.handleCompensatePayment(event);
        } catch (Exception ex) {
            log.error("Error handling CompensatePaymentEvent for paymentId {}: {}", event.getPaymentId(), ex.getMessage(), ex);
        }
    }
}
