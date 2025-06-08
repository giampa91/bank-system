package com.bank.system.account_service.service;

import com.bank.system.account_service.controller.AccountController;
import com.bank.system.account_service.domain.Account;
import com.bank.system.account_service.repository.AccountRepository;
import com.bank.system.dtos.dto.PaymentInitiatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private final AccountRepository accountRepository;
    private final PaymentAccountService paymentAccountService;

    public AccountService(AccountRepository accountRepository, PaymentAccountService paymentAccountService) {
        this.accountRepository = accountRepository;
        this.paymentAccountService = paymentAccountService;
    }

    @Transactional
    public CompletableFuture<Account> createAccount(Account account) {
        if (account.getBalance() == null || account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial balance cannot be null or negative.");
        }
        log.info("Attempting to create account for user ID: {}", account.getUserId());
        return accountRepository.save(account)
                .exceptionally(ex -> {
                    log.error("Failed to create account for user ID {}: {}", account.getUserId(), ex.getMessage());
                    throw new RuntimeException("Account creation failed", ex);
                });
    }

    public CompletableFuture<Optional<Account>> getAccountById(Long id) {
        log.debug("Fetching account by ID: {}", id);
        return accountRepository.findById(id)
                .exceptionally(ex -> {
                    log.error("Failed to fetch account by ID {}: {}", id, ex.getMessage());
                    throw new RuntimeException("Failed to retrieve account by ID", ex);
                });
    }

    public CompletableFuture<Optional<Account>> getAccountByAccountNumber(String accountNumber) {
        log.debug("Fetching account by account number: {}", accountNumber);
        return accountRepository.findByAccountNumber(accountNumber)
                .exceptionally(ex -> {
                    log.error("Failed to fetch account by account number {}: {}", accountNumber, ex.getMessage());
                    throw new RuntimeException("Failed to retrieve account by account number", ex);
                });
    }

    @Transactional
    public CompletableFuture<Account> deposit(String accountNumber, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }

        return accountRepository.findByAccountNumber(accountNumber)
                .thenCompose(optionalAccount -> {
                    if (optionalAccount.isEmpty()) {
                        log.warn("Deposit failed: Account {} not found.", accountNumber);
                        throw new RuntimeException("Account not found for deposit.");
                    }
                    Account account = optionalAccount.get();
                    BigDecimal newBalance = account.getBalance().add(amount);
                    log.info("Depositing {} into account {}. New balance: {}", amount, accountNumber, newBalance);
                    return accountRepository.updateBalance(accountNumber, newBalance)
                            .thenApply(updatedOptional -> updatedOptional.orElseThrow(() -> new RuntimeException("Failed to update account balance after deposit.")));
                })
                .exceptionally(ex -> {
                    log.error("Error during deposit into account {}: {}", accountNumber, ex.getMessage());
                    throw new RuntimeException("Deposit operation failed", ex);
                });
    }

    @Transactional
    public CompletableFuture<Account> withdraw(String accountNumber, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive.");
        }

        return accountRepository.findByAccountNumber(accountNumber)
                .thenCompose(optionalAccount -> {
                    if (optionalAccount.isEmpty()) {
                        log.warn("Withdrawal failed: Account {} not found.", accountNumber);
                        throw new RuntimeException("Account not found for withdrawal.");
                    }
                    Account account = optionalAccount.get();
                    if (account.getBalance().compareTo(amount) < 0) {
                        log.warn("Withdrawal failed: Insufficient funds in account {}. Current balance: {}, requested: {}",
                                accountNumber, account.getBalance(), amount);
                        throw new RuntimeException("Insufficient funds.");
                    }
                    BigDecimal newBalance = account.getBalance().subtract(amount);
                    log.info("Withdrawing {} from account {}. New balance: {}", amount, accountNumber, newBalance);
                    return accountRepository.updateBalance(accountNumber, newBalance)
                            .thenApply(updatedOptional -> updatedOptional.orElseThrow(() -> new RuntimeException("Failed to update account balance after withdrawal.")));
                })
                .exceptionally(ex -> {
                    log.error("Error during withdrawal from account {}: {}", accountNumber, ex.getMessage());
                    throw new RuntimeException("Withdrawal operation failed", ex);
                });
    }

    @Transactional
    public CompletableFuture<Boolean> deleteAccount(String accountNumber) {
        log.info("Attempting to delete account: {}", accountNumber);
        return accountRepository.deleteByAccountNumber(accountNumber)
                .exceptionally(ex -> {
                    log.error("Failed to delete account {}: {}", accountNumber, ex.getMessage());
                    throw new RuntimeException("Account deletion failed", ex);
                });
    }
}