package com.bank.system.account_service.service;

import com.bank.system.account_service.domain.Account;
import com.bank.system.account_service.repository.AccountRepository;
import com.bank.system.dtos.dto.PaymentInitiatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

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
    public Account createAccount(Account account) {
        if (account.getBalance() == null || account.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial balance cannot be null or negative.");
        }
        log.info("Attempting to create account for user ID: {}", account.getUserId());
        try {
            return accountRepository.save(account);
        } catch (Exception ex) {
            log.error("Failed to create account for user ID {}: {}", account.getUserId(), ex.getMessage());
            throw new RuntimeException("Account creation failed", ex);
        }
    }

    public Optional<Account> getAccountById(Long id) {
        log.debug("Fetching account by ID: {}", id);
        try {
            return accountRepository.findById(id);
        } catch (Exception ex) {
            log.error("Failed to fetch account by ID {}: {}", id, ex.getMessage());
            throw new RuntimeException("Failed to retrieve account by ID", ex);
        }
    }

    public Optional<Account> getAccountByAccountNumber(String accountNumber) {
        log.debug("Fetching account by account number: {}", accountNumber);
        try {
            return accountRepository.findByAccountNumber(accountNumber);
        } catch (Exception ex) {
            log.error("Failed to fetch account by account number {}: {}", accountNumber, ex.getMessage());
            throw new RuntimeException("Failed to retrieve account by account number", ex);
        }
    }

    @Transactional
    public Account deposit(String accountNumber, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }

        try {
            Optional<Account> optionalAccount = accountRepository.findByAccountNumber(accountNumber);
            if (optionalAccount.isEmpty()) {
                log.warn("Deposit failed: Account {} not found.", accountNumber);
                throw new RuntimeException("Account not found for deposit.");
            }

            Account account = optionalAccount.get();
            BigDecimal newBalance = account.getBalance().add(amount);
            log.info("Depositing {} into account {}. New balance: {}", amount, accountNumber, newBalance);

            Optional<Account> updatedAccount = accountRepository.updateBalance(accountNumber, newBalance);
            return updatedAccount.orElseThrow(() -> new RuntimeException("Failed to update account balance after deposit."));
        } catch (Exception ex) {
            log.error("Error during deposit into account {}: {}", accountNumber, ex.getMessage());
            throw new RuntimeException("Deposit operation failed", ex);
        }
    }

    @Transactional
    public Account withdraw(String accountNumber, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive.");
        }

        try {
            Optional<Account> optionalAccount = accountRepository.findByAccountNumber(accountNumber);
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

            Optional<Account> updatedAccount = accountRepository.updateBalance(accountNumber, newBalance);
            return updatedAccount.orElseThrow(() -> new RuntimeException("Failed to update account balance after withdrawal."));
        } catch (Exception ex) {
            log.error("Error during withdrawal from account {}: {}", accountNumber, ex.getMessage());
            throw new RuntimeException("Withdrawal operation failed", ex);
        }
    }

    @Transactional
    public boolean deleteAccount(String accountNumber) {
        log.info("Attempting to delete account: {}", accountNumber);
        try {
            return accountRepository.deleteByAccountNumber(accountNumber);
        } catch (Exception ex) {
            log.error("Failed to delete account {}: {}", accountNumber, ex.getMessage());
            throw new RuntimeException("Account deletion failed", ex);
        }
    }
}
