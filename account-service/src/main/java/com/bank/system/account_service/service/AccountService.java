package com.bank.system.account_service.service;

import com.bank.system.account_service.domain.Account;
import com.bank.system.account_service.kafka.AccountProducer;
import com.bank.system.account_service.repository.AccountRepository;
import com.bank.system.dtos.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
        String senderAccountId = paymentInitiatedEvent.getSenderAccountId();
        BigDecimal debitAmount = paymentInitiatedEvent.getAmount();
        String paymentId = paymentInitiatedEvent.getPaymentId();

        log.info("Attempting to debit sender account {} for payment ID {}", senderAccountId, paymentId);

        return accountRepository.findByAccountNumber(senderAccountId)
                .thenCompose(optionalAccount -> {
                    if (optionalAccount.isEmpty()) {
                        String errorMsg = String.format("Sender account %s not found for payment ID %s. Debit failed.", senderAccountId, paymentId);
                        log.error(errorMsg);
                        // In a real system, you might send a PaymentFailedEvent here
                        return CompletableFuture.failedFuture(new RuntimeException(errorMsg));
                    }

                    Account senderAccount = optionalAccount.get();
                    if (senderAccount.getBalance().compareTo(debitAmount) < 0) {
                        String errorMsg = String.format("Insufficient funds in sender account %s (balance: %s) for payment ID %s (amount: %s). Debit failed.",
                                senderAccountId, senderAccount.getBalance(), paymentId, debitAmount);
                        log.error(errorMsg);
                        // In a real system, you might send a PaymentFailedEvent here
                        return CompletableFuture.failedFuture(new RuntimeException(errorMsg));
                    }

                    BigDecimal newBalance = senderAccount.getBalance().subtract(debitAmount);
                    log.info("Debiting sender account {}. Old balance: {}, New balance: {}", senderAccountId, senderAccount.getBalance(), newBalance);
                    return accountRepository.updateBalance(senderAccountId, newBalance)
                            .thenCompose(updatedOptionalAccount -> {
                                if (updatedOptionalAccount.isPresent()) {
                                    SenderDebitedEvent senderDebitedEvent = mapPaymentInitiatedEventToSenderDebitedEvent(paymentInitiatedEvent);
                                    return accountProducer.sendSenderDebitedEvent(senderDebitedEvent)
                                            .thenAccept(sendResult -> log.info("SenderDebitedEvent published for paymentId: {} on account: {}", paymentId, senderAccountId))
                                            .exceptionally(ex -> {
                                                log.error("Failed to publish SenderDebitedEvent for paymentId {} on account {}: {}", paymentId, senderAccountId, ex.getMessage(), ex);
                                                // Consider compensation logic here if Kafka send fails after successful debit
                                                throw new RuntimeException("Failed to publish SenderDebitedEvent", ex);
                                            });
                                } else {
                                    String errorMsg = String.format("Failed to update balance for sender account %s for payment ID %s. Account not found after initial check.", senderAccountId, paymentId);
                                    log.error(errorMsg);
                                    return CompletableFuture.failedFuture(new RuntimeException(errorMsg));
                                }
                            });
                })
                .exceptionally(ex -> {
                    log.error("Error during debit process for payment ID {}: {}", paymentId, ex.getMessage(), ex);
                    // This catches exceptions from findByAccountNumber, balance check, or updateBalance
                    return null; // Or rethrow if you want to propagate specific errors further
                });
    }

    public CompletableFuture<Void> handleReceiverCreditRequestEvent(ReceiverCreditRequestEvent receiverCreditRequestEvent) {
        String receiverAccountId = receiverCreditRequestEvent.getAccountId();
        BigDecimal creditAmount = receiverCreditRequestEvent.getCreditedAmount();
        String paymentId = receiverCreditRequestEvent.getPaymentId();

        log.info("Attempting to credit receiver account {} for payment ID {}", receiverAccountId, paymentId);

        return accountRepository.findByAccountNumber(receiverAccountId)
                .thenCompose(optionalAccount -> {
                    if (optionalAccount.isEmpty()) {
                        String errorMsg = String.format("Receiver account %s not found for payment ID %s. Credit failed.", receiverAccountId, paymentId);
                        log.error(errorMsg);
                        // In a real system, you might send a PaymentFailedEvent here if this was critical
                        return CompletableFuture.failedFuture(new RuntimeException(errorMsg));
                    }

                    Account receiverAccount = optionalAccount.get();
                    BigDecimal newBalance = receiverAccount.getBalance().add(creditAmount);
                    log.info("Crediting receiver account {}. Old balance: {}, New balance: {}", receiverAccountId, receiverAccount.getBalance(), newBalance);

                    return accountRepository.updateBalance(receiverAccountId, newBalance)
                            .thenCompose(updatedOptionalAccount -> {
                                if (updatedOptionalAccount.isPresent()) {
                                    ReceiverCreditEvent receiverCreditEvent = mapReceiverCreditRequestEventToReceiverCreditEvent(receiverCreditRequestEvent);
                                    return accountProducer.sendReceiverCreditEvent(receiverCreditEvent)
                                            .thenAccept(sendResult -> log.info("ReceiverCreditEvent published for paymentId: {} on account: {}", paymentId, receiverAccountId))
                                            .exceptionally(ex -> {
                                                log.error("Failed to publish ReceiverCreditEvent for paymentId {} on account {}: {}", paymentId, receiverAccountId, ex.getMessage(), ex);
                                                // Consider compensation logic here if Kafka send fails after successful credit
                                                throw new RuntimeException("Failed to publish ReceiverCreditEvent", ex);
                                            });
                                } else {
                                    String errorMsg = String.format("Failed to update balance for receiver account %s for payment ID %s. Account not found after initial check.", receiverAccountId, paymentId);
                                    log.error(errorMsg);
                                    return CompletableFuture.failedFuture(new RuntimeException(errorMsg));
                                }
                            });
                })
                .exceptionally(ex -> {
                    log.error("Error during credit process for payment ID {}: {}", paymentId, ex.getMessage(), ex);
                    // This catches exceptions from findByAccountNumber or updateBalance
                    return null; // Or rethrow
                });
    }

    public CompletableFuture<Void> handleCompensatePaymentRequestEvent(CompensatePaymentRequestEvent event) {

        String accountNumber = event.getAccountId();
        String paymentId = event.getPaymentId();
        BigDecimal amount = event.getAmount();

        log.info("Attempting to compensate debit to account number {} for payment ID {}", accountNumber, paymentId);

        return accountRepository.findByAccountNumber(accountNumber)
                .thenCompose(optionalAccount -> {
                    if (optionalAccount.isEmpty()) {
                        String errorMsg = String.format("Receiver account %s not found for payment ID %s. compensation failed.", accountNumber, paymentId);
                        log.error(errorMsg);
                        // In a real system, you might send a PaymentFailedEvent here if this was critical
                        return CompletableFuture.failedFuture(new RuntimeException(errorMsg));
                    }

                    Account receiverAccount = optionalAccount.get();
                    BigDecimal newBalance = receiverAccount.getBalance().add(amount);
                    log.info("Crediting receiver account {}. Old balance: {}, New balance: {}", accountNumber, receiverAccount.getBalance(), newBalance);

                    return accountRepository.updateBalance(accountNumber, newBalance)
                            .thenCompose(updatedOptionalAccount -> {
                                if (updatedOptionalAccount.isPresent()) {
                                    CompensatePaymentEvent compensatePaymentEvent = mapCompensatePaymentRequestEventToCompensatePaymentEvent(event);
                                    return accountProducer.sendCompensatePaymentEvent(compensatePaymentEvent)
                                            .thenAccept(sendResult -> log.info("CompensatePaymentEvent published for paymentId: {} on account: {}", paymentId, accountNumber))
                                            .exceptionally(ex -> {
                                                log.error("Failed to publish CompensatePaymentEvent for paymentId {} on account {}: {}", paymentId, accountNumber, ex.getMessage(), ex);
                                                throw new RuntimeException("Failed to publish CompensatePaymentEvent", ex);
                                            });
                                } else {
                                    String errorMsg = String.format("Failed to compensate and update balance for receiver account %s for payment ID %s. Account not found after initial check.", accountNumber, paymentId);
                                    log.error(errorMsg);
                                    return CompletableFuture.failedFuture(new RuntimeException(errorMsg));
                                }
                            });
                })
                .exceptionally(ex -> {
                    log.error("Error during compensate process for payment ID {}: {}", paymentId, ex.getMessage(), ex);
                    // This catches exceptions from findByAccountNumber or updateBalance
                    return null; // Or rethrow
                });
    }

    private static SenderDebitedEvent mapPaymentInitiatedEventToSenderDebitedEvent(PaymentInitiatedEvent paymentInitiatedEvent) {
        SenderDebitedEvent senderDebitedEvent = new SenderDebitedEvent();
        senderDebitedEvent.setAccountId(paymentInitiatedEvent.getSenderAccountId());
        senderDebitedEvent.setDebitedAmount(paymentInitiatedEvent.getAmount());
        senderDebitedEvent.setPaymentId(paymentInitiatedEvent.getPaymentId());
        senderDebitedEvent.setCurrency(paymentInitiatedEvent.getCurrency());
        senderDebitedEvent.setTimestamp(Instant.now());
        return senderDebitedEvent;
    }

    private static ReceiverCreditEvent mapReceiverCreditRequestEventToReceiverCreditEvent(ReceiverCreditRequestEvent receiverCreditRequestEvent) {
        ReceiverCreditEvent receiverCreditEvent = new ReceiverCreditEvent();
        receiverCreditEvent.setAccountId(receiverCreditRequestEvent.getAccountId());
        receiverCreditEvent.setCreditedAmount(receiverCreditRequestEvent.getCreditedAmount());
        receiverCreditEvent.setPaymentId(receiverCreditRequestEvent.getPaymentId());
        return receiverCreditEvent;
    }

    private static CompensatePaymentEvent mapCompensatePaymentRequestEventToCompensatePaymentEvent(CompensatePaymentRequestEvent event) {
        CompensatePaymentEvent compensatePaymentEvent = new CompensatePaymentEvent();
        compensatePaymentEvent.setAccountId(event.getAccountId());
        compensatePaymentEvent.setAmount(event.getAmount());
        compensatePaymentEvent.setPaymentId(event.getPaymentId());
        compensatePaymentEvent.setReason(event.getReason());
        return compensatePaymentEvent;
    }
}