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
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentAccountService {

    private static final Logger log = LoggerFactory.getLogger(PaymentAccountService.class);

    private final AccountProducer accountProducer;
    private final AccountRepository accountRepository;

    public PaymentAccountService(AccountProducer accountProducer, AccountRepository accountRepository) {
        this.accountProducer = accountProducer;
        this.accountRepository = accountRepository;
    }

    public boolean handlePaymentInitiatedEvent(PaymentInitiatedEvent paymentInitiatedEvent) {
        String senderAccountId = paymentInitiatedEvent.getSenderAccountId();
        BigDecimal debitAmount = paymentInitiatedEvent.getAmount();
        UUID paymentId = paymentInitiatedEvent.getPaymentId();

        log.info("Attempting to debit sender account {} for payment ID {}", senderAccountId, paymentId);

        try {
            Optional<Account> optionalAccount = accountRepository.findByAccountNumber(senderAccountId);
            if (optionalAccount.isEmpty()) {
                String errorMsg = String.format("Sender account %s not found for payment ID %s. Debit failed.", senderAccountId, paymentId);
                log.error(errorMsg);
                DebitFailedEvent debitFailedEvent = mapPaymentInitiatedEventToDebitFailedEvent(paymentInitiatedEvent, errorMsg);
                accountProducer.sendSenderDebitedFailedEvent(debitFailedEvent);
                return false;
            }

            Account senderAccount = optionalAccount.get();
            if (senderAccount.getBalance().compareTo(debitAmount) < 0) {
                String errorMsg = String.format("Insufficient funds in sender account %s (balance: %s) for payment ID %s (amount: %s). Debit failed.",
                        senderAccountId, senderAccount.getBalance(), paymentId, debitAmount);
                log.error(errorMsg);
                DebitFailedEvent debitFailedEvent = mapPaymentInitiatedEventToDebitFailedEvent(paymentInitiatedEvent, errorMsg);
                accountProducer.sendSenderDebitedFailedEvent(debitFailedEvent);
                return false;
            }

            BigDecimal newBalance = senderAccount.getBalance().subtract(debitAmount);
            log.info("Debiting sender account {}. Old balance: {}, New balance: {}", senderAccountId, senderAccount.getBalance(), newBalance);

            Optional<Account> updatedOptionalAccount = accountRepository.updateBalance(senderAccountId, newBalance);
            if (updatedOptionalAccount.isPresent()) {
                SenderDebitedEvent senderDebitedEvent = mapPaymentInitiatedEventToSenderDebitedEvent(paymentInitiatedEvent);
                accountProducer.sendSenderDebitedEvent(senderDebitedEvent);
                log.info("SenderDebitedEvent published for paymentId: {} on account: {}", paymentId, senderAccountId);
                return true;
            } else {
                String errorMsg = String.format("Failed to update balance for sender account %s for payment ID %s. Account not found after initial check.", senderAccountId, paymentId);
                log.error(errorMsg);
                DebitFailedEvent debitFailedEvent = mapPaymentInitiatedEventToDebitFailedEvent(paymentInitiatedEvent, errorMsg);
                accountProducer.sendSenderDebitedFailedEvent(debitFailedEvent);
                return false;
            }
        } catch (Exception ex) {
            log.error("Critical error during debit process for payment ID {}: {}", paymentId, ex.getMessage(), ex);
            return false;
        }
    }

    public void handleReceiverCreditRequestEvent(ReceiverCreditRequestEvent event) {
        String receiverAccountId = event.getAccountId();
        BigDecimal creditAmount = event.getCreditedAmount();
        UUID paymentId = event.getPaymentId();

        log.info("Attempting to credit receiver account {} for payment ID {}", receiverAccountId, paymentId);

        try {
            Optional<Account> optionalAccount = accountRepository.findByAccountNumber(receiverAccountId);
            if (optionalAccount.isEmpty()) {
                String errorMsg = String.format("Receiver account %s not found for payment ID %s. Credit failed.", receiverAccountId, paymentId);
                log.error(errorMsg);
                DebitFailedEvent debitFailedEvent = mapReceiverCreditRequestEventToDebitFailedEvent(event, errorMsg);
                accountProducer.sendSenderDebitedFailedEvent(debitFailedEvent);
                return;
            }

            Account receiverAccount = optionalAccount.get();
            BigDecimal newBalance = receiverAccount.getBalance().add(creditAmount);
            log.info("Crediting receiver account {}. Old balance: {}, New balance: {}", receiverAccountId, receiverAccount.getBalance(), newBalance);

            Optional<Account> updatedOptionalAccount = accountRepository.updateBalance(receiverAccountId, newBalance);
            if (updatedOptionalAccount.isPresent()) {
                ReceiverCreditEvent receiverCreditEvent = mapReceiverCreditRequestEventToReceiverCreditEvent(event);
                accountProducer.sendReceiverCreditEvent(receiverCreditEvent);
                log.info("ReceiverCreditEvent published for paymentId: {} on account: {}", paymentId, receiverAccountId);
            } else {
                String errorMsg = String.format("Failed to update balance for receiver account %s for payment ID %s.", receiverAccountId, paymentId);
                log.error(errorMsg);
                DebitFailedEvent debitFailedEvent = mapReceiverCreditRequestEventToDebitFailedEvent(event, errorMsg);
                accountProducer.sendSenderDebitedFailedEvent(debitFailedEvent);
            }
        } catch (Exception ex) {
            log.error("Error during credit process for payment ID {}: {}", paymentId, ex.getMessage(), ex);
            DebitFailedEvent debitFailedEvent = mapReceiverCreditRequestEventToDebitFailedEvent(event, ex.getMessage());
            accountProducer.sendSenderDebitedFailedEvent(debitFailedEvent);
        }
    }

    public void handleCompensatePaymentRequestEvent(CompensatePaymentRequestEvent event) {
        String accountNumber = event.getAccountId();
        UUID paymentId = event.getPaymentId();
        BigDecimal amount = event.getAmount();

        log.info("Attempting to compensate debit to account number {} for payment ID {}", accountNumber, paymentId);

        try {
            Optional<Account> optionalAccount = accountRepository.findByAccountNumber(accountNumber);
            if (optionalAccount.isEmpty()) {
                throw new RuntimeException(String.format("Receiver account %s not found for payment ID %s. Compensation failed.", accountNumber, paymentId));
            }

            Account receiverAccount = optionalAccount.get();
            BigDecimal newBalance = receiverAccount.getBalance().add(amount);
            log.info("Crediting receiver account {}. Old balance: {}, New balance: {}", accountNumber, receiverAccount.getBalance(), newBalance);

            Optional<Account> updatedOptionalAccount = accountRepository.updateBalance(accountNumber, newBalance);
            if (updatedOptionalAccount.isPresent()) {
                CompensatePaymentEvent compensatePaymentEvent = mapCompensatePaymentRequestEventToCompensatePaymentEvent(event);
                accountProducer.sendCompensatePaymentEvent(compensatePaymentEvent);
                log.info("CompensatePaymentEvent published for paymentId: {} on account: {}", paymentId, accountNumber);
            } else {
                throw new RuntimeException(String.format("Failed to update balance for receiver account %s for payment ID %s.", accountNumber, paymentId));
            }
        } catch (Exception ex) {
            log.error("Error during compensate process for payment ID {}: {}", paymentId, ex.getMessage(), ex);
        }
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

    private static DebitFailedEvent mapPaymentInitiatedEventToDebitFailedEvent(PaymentInitiatedEvent paymentInitiatedEvent, String errorMsg) {
        DebitFailedEvent debitFailedEvent = new DebitFailedEvent();
        debitFailedEvent.setPaymentId(paymentInitiatedEvent.getPaymentId());
        debitFailedEvent.setReason(errorMsg);
        debitFailedEvent.setAccountId(paymentInitiatedEvent.getSenderAccountId());
        debitFailedEvent.setTimestamp(Instant.now());
        return debitFailedEvent;
    }

    private static DebitFailedEvent mapReceiverCreditRequestEventToDebitFailedEvent(ReceiverCreditRequestEvent receiverCreditRequestEvent, String errorMsg) {
        DebitFailedEvent debitFailedEvent = new DebitFailedEvent();
        debitFailedEvent.setPaymentId(receiverCreditRequestEvent.getPaymentId());
        debitFailedEvent.setReason(errorMsg);
        debitFailedEvent.setAccountId(receiverCreditRequestEvent.getAccountId());
        debitFailedEvent.setTimestamp(Instant.now());
        return debitFailedEvent;
    }
}