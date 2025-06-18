package com.bank.system.account_service.controller;

import com.bank.system.account_service.domain.Account;
import com.bank.system.account_service.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = "http://localhost:3000")
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody Account account) {
        log.info("Received request to create account for user ID: {}", account.getUserId());
        try {
            Account createdAccount = accountService.createAccount(account);
            log.info("Account created successfully with ID: {}", createdAccount.getId());
            return new ResponseEntity<>(createdAccount, HttpStatus.CREATED);
        } catch (Exception ex) {
            log.error("Error creating account: {}", ex.getMessage());
            if (ex.getCause() instanceof IllegalArgumentException) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccountById(@PathVariable Long id) {
        log.info("Received request to get account by ID: {}", id);
        try {
            Optional<Account> optionalAccount = accountService.getAccountById(id);
            return optionalAccount
                    .map(account -> {
                        log.info("Account found for ID: {}", id);
                        return new ResponseEntity<>(account, HttpStatus.OK);
                    })
                    .orElseGet(() -> {
                        log.warn("Account not found for ID: {}", id);
                        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                    });
        } catch (Exception ex) {
            log.error("Error retrieving account by ID {}: {}", id, ex.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/by-account-number/{accountNumber}")
    public ResponseEntity<Account> getAccountByAccountNumber(@PathVariable String accountNumber) {
        log.info("Received request to get account by account number: {}", accountNumber);
        try {
            Optional<Account> optionalAccount = accountService.getAccountByAccountNumber(accountNumber);
            return optionalAccount
                    .map(account -> {
                        log.info("Account found for account number: {}", accountNumber);
                        return new ResponseEntity<>(account, HttpStatus.OK);
                    })
                    .orElseGet(() -> {
                        log.warn("Account not found for account number: {}", accountNumber);
                        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                    });
        } catch (Exception ex) {
            log.error("Error retrieving account by account number {}: {}", accountNumber, ex.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<Account> deposit(
            @PathVariable String accountNumber,
            @RequestBody DepositRequest request) {
        log.info("Received request to deposit {} into account {}", request.getAmount(), accountNumber);
        try {
            return Optional.of(accountService.deposit(accountNumber, request.getAmount()))
                    .map(account -> {
                        log.info("Deposit successful for account {}. New balance: {}", accountNumber, account.getBalance());
                        return new ResponseEntity<>(account, HttpStatus.OK);
                    })
                    .orElseGet(() -> {
                        log.warn("Deposit failed: Account {} not found (after service call).", accountNumber);
                        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                    });
        } catch (Exception ex) {
            log.error("Error during deposit into account {}: {}", accountNumber, ex.getMessage());
            if (ex.getCause() instanceof IllegalArgumentException) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            } else if (ex.getCause() instanceof RuntimeException &&
                    ex.getCause().getMessage().contains("Account not found")) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{accountNumber}/withdraw")
    public ResponseEntity<Account> withdraw(
            @PathVariable String accountNumber,
            @RequestBody WithdrawRequest request) {
        log.info("Received request to withdraw {} from account {}", request.getAmount(), accountNumber);
        try {
            return Optional.of(accountService.deposit(accountNumber, request.getAmount()))
                    .map(account -> {
                        log.info("Withdrawal successful for account {}. New balance: {}", accountNumber, account.getBalance());
                        return new ResponseEntity<>(account, HttpStatus.OK);
                    })
                    .orElseGet(() -> {
                        log.warn("Withdrawal failed: Account {} not found (after service call).", accountNumber);
                        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                    });
        } catch (Exception ex) {
            log.error("Error during withdrawal from account {}: {}", accountNumber, ex.getMessage());
            if (ex.getCause() instanceof IllegalArgumentException) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            } else if (ex.getCause() instanceof RuntimeException) {
                String message = ex.getCause().getMessage();
                if (message.contains("Insufficient funds")) {
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                } else if (message.contains("Account not found")) {
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }
            }
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{accountNumber}")
    public ResponseEntity<Object> deleteAccount(@PathVariable String accountNumber) {
        log.info("Received request to delete account: {}", accountNumber);
        try {
            boolean deleted = accountService.deleteAccount(accountNumber);
            if (deleted) {
                log.info("Account {} deleted successfully.", accountNumber);
                return ResponseEntity.noContent().build();
            } else {
                log.warn("Account {} not found for deletion.", accountNumber);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            log.error("Error deleting account {}: {}", accountNumber, ex.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // --- DTOs ---

    public static class DepositRequest {
        private BigDecimal amount;

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    public static class WithdrawRequest {
        private BigDecimal amount;

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    public static class TransferRequest {
        private String toAccountNumber;
        private String idempotencyKey;
        private BigDecimal amount;

        public String getToAccountNumber() {
            return toAccountNumber;
        }

        public void setToAccountNumber(String toAccountNumber) {
            this.toAccountNumber = toAccountNumber;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getIdempotencyKey() {
            return idempotencyKey;
        }
    }
}
