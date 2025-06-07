package com.bank.system.account_service.kafka;

import com.bank.system.account_service.service.AccountService;
import com.bank.system.account_service.service.PaymentAccountService;
import com.bank.system.dtos.dto.CompensatePaymentRequestEvent;
import com.bank.system.dtos.dto.PaymentInitiatedEvent;
import com.bank.system.dtos.dto.ReceiverCreditRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AccountConsumer {

    private static final Logger log = LoggerFactory.getLogger(AccountConsumer.class);
    public static final String SPRING_KAFKA_CONSUMER_GROUP_ID = "spring.kafka.consumer.group-id";

    private static final String PAYMENT_INITIATED_TOPIC = "payment-initiated-topic";
    private static final String RECEIVER_CREDITED_REQUESTED_TOPIC = "receiver-credited-requested-topic";
    private static final String COMPENSATE_PAYMENT_REQEUST_TOPIC = "compensate-payment-request-topic";

    private final PaymentAccountService paymentAccountService;

    @Autowired
    public AccountConsumer(PaymentAccountService paymentAccountService){
        this.paymentAccountService = paymentAccountService;
    }


    @KafkaListener(topics = PAYMENT_INITIATED_TOPIC, groupId = "${" + AccountConsumer.SPRING_KAFKA_CONSUMER_GROUP_ID + "}")
    public void listenPaymentInitiatedEvent(PaymentInitiatedEvent event) {
        log.info("Consumed PaymentInitiatedEvent for paymentId: {}", event.getPaymentId());
        paymentAccountService.handlePaymentInitiatedEvent(event);
    }

    @KafkaListener(topics = RECEIVER_CREDITED_REQUESTED_TOPIC, groupId = "${" + AccountConsumer.SPRING_KAFKA_CONSUMER_GROUP_ID + "}")
    public void listenReceiverCreditedRequestedEvent(ReceiverCreditRequestEvent event) {
        log.info("Consumed ReceiverCreditRequestEvent for paymentId: {}", event.getPaymentId());
        paymentAccountService.handleReceiverCreditRequestEvent(event);
    }

    @KafkaListener(topics = COMPENSATE_PAYMENT_REQEUST_TOPIC, groupId = "${" + AccountConsumer.SPRING_KAFKA_CONSUMER_GROUP_ID + "}")
    public void listenCompensatePaymentRequestEvent(CompensatePaymentRequestEvent event) {
        log.info("Consumed CompensatePaymentEvent for paymentId: {}", event.getPaymentId());
        paymentAccountService.handleCompensatePaymentRequestEvent(event);
    }

}
