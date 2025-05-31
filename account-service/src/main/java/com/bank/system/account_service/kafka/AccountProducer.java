package com.bank.system.account_service.kafka;

import com.bank.system.dtos.dto.PaymentInitiatedEvent;
import com.bank.system.dtos.dto.ReceiverCreditEvent;
import com.bank.system.dtos.dto.SenderDebitedEvent;
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

}