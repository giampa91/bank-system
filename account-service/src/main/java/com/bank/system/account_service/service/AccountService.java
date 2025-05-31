package com.bank.system.account_service.service;

import com.bank.system.account_service.kafka.AccountProducer;
import com.bank.system.account_service.repository.AccountRepository;
import com.bank.system.dtos.dto.PaymentInitiatedEvent;
import com.bank.system.dtos.dto.ReceiverCreditEvent;
import com.bank.system.dtos.dto.ReceiverCreditRequestEvent;
import com.bank.system.dtos.dto.SenderDebitedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountProducer accountProducer;
    private final AccountRepository accountRepository;

    public AccountService(AccountProducer accountProducer, AccountRepository accountRepository) {
        this.accountProducer = accountProducer;
        this.accountRepository = accountRepository;
    }


    public CompletableFuture<Void> handlePaymentInitiatedEvent(PaymentInitiatedEvent paymentInitiatedEvent) {
        SenderDebitedEvent senderDebitedEvent = mapPaymentToPaymentCompletedEvent(paymentInitiatedEvent);
        return accountProducer.sendSenderDebitedEvent(senderDebitedEvent)
                .thenAccept(sendResult -> log.info("PaymentInitiatedEvent published for paymentId: {}", senderDebitedEvent.getPaymentId()))
                .exceptionally(ex -> {
                    log.error("Failed to publish PaymentCompletedEvent for paymentId {}: {}", senderDebitedEvent.getPaymentId(), ex.getMessage(), ex);
                    // Handle failure to publish completion event (e.g., retry, alert)
                    return null;
                });
    }

    public CompletableFuture<Void> handleReceiverCreditRequestEvent(ReceiverCreditRequestEvent receiverCreditRequestEvent) {
        ReceiverCreditEvent receiverCreditEvent = mapReceiverCreditRequestEventToReceiverCreditEvent(receiverCreditRequestEvent);
        return accountProducer.sendReceiverCreditEvent(receiverCreditEvent)
                .thenAccept(sendResult -> log.info("PaymentCompletedEvent published for paymentId: {}", receiverCreditEvent.getPaymentId()))
                .exceptionally(ex -> {
                    log.error("Failed to publish PaymentCompletedEvent for paymentId {}: {}", receiverCreditEvent.getPaymentId(), ex.getMessage(), ex);
                    // Handle failure to publish completion event (e.g., retry, alert)
                    return null;
                });
    }

    private static ReceiverCreditEvent mapReceiverCreditRequestEventToReceiverCreditEvent(ReceiverCreditRequestEvent receiverCreditRequestEvent) {
        ReceiverCreditEvent receiverCreditEvent = new ReceiverCreditEvent();
        receiverCreditEvent.setAccountId(receiverCreditRequestEvent.getAccountId());
        receiverCreditEvent.setCreditedAmount(receiverCreditRequestEvent.getCreditedAmount());
        receiverCreditEvent.setPaymentId(receiverCreditRequestEvent.getPaymentId());
        return receiverCreditEvent;
    }

    private static SenderDebitedEvent mapPaymentToPaymentCompletedEvent(PaymentInitiatedEvent paymentInitiatedEvent) {
        SenderDebitedEvent senderDebitedEvent = new SenderDebitedEvent();
        // set account id to sender because he has the debit
        senderDebitedEvent.setAccountId(paymentInitiatedEvent.getSenderAccountId());
        senderDebitedEvent.setDebitedAmount(paymentInitiatedEvent.getAmount());
        senderDebitedEvent.setPaymentId(paymentInitiatedEvent.getPaymentId());
        senderDebitedEvent.setCurrency(paymentInitiatedEvent.getCurrency());
        senderDebitedEvent.setTimestamp(Instant.now());
        return senderDebitedEvent;
    }

}