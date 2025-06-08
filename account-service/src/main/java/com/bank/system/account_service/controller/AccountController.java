// src/main/java/com/bank/system/account_service/controller/AccountController.java
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
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for managing bank accounts.
 * Provides API endpoints for creating, retrieving, updating (deposit/withdraw/transfer),
 * and deleting accounts.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);
    private final AccountService accountService;

    /**
     * Constructor for AccountController.
     * @param accountService The service layer for account operations.
     */
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Creates a new bank account.
     * HTTP Method: POST
     * Endpoint: /api/accounts
     * Request Body: Account object (JSON)
     * Response: Created Account object with HTTP status 201 (Created).
     *
     * @param account The Account object to be created.
     * @return A CompletableFuture containing ResponseEntity with the created Account and HTTP status.
     */
    @PostMapping
    public CompletableFuture<ResponseEntity<Account>> createAccount(@RequestBody Account account) {
        log.info("Received request to create account for user ID: {}", account.getUserId());
        return accountService.createAccount(account)
                .thenApply(createdAccount -> {
                    log.info("Account created successfully with ID: {}", createdAccount.getId());
                    return new ResponseEntity<>(createdAccount, HttpStatus.CREATED);
                })
                .exceptionally(ex -> {
                    log.error("Error creating account: {}", ex.getMessage());
                    // Catches IllegalArgumentException for validation errors
                    if (ex.getCause() instanceof IllegalArgumentException) {
                        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                    }
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }

    /**
     * Retrieves an account by its unique ID.
     * HTTP Method: GET
     * Endpoint: /api/accounts/{id}
     * Response: Account object with HTTP status 200 (OK) if found, 404 (Not Found) otherwise.
     *
     * @param id The internal ID of the account to retrieve.
     * @return A CompletableFuture containing ResponseEntity with the Account and HTTP status.
     */
    @GetMapping("/{id}")
    public CompletableFuture<ResponseEntity<Account>> getAccountById(@PathVariable Long id) {
        log.info("Received request to get account by ID: {}", id);
        return accountService.getAccountById(id)
                .thenApply(optionalAccount -> optionalAccount
                        .map(account -> {
                            log.info("Account found for ID: {}", id);
                            return new ResponseEntity<>(account, HttpStatus.OK);
                        })
                        .orElseGet(() -> {
                            log.warn("Account not found for ID: {}", id);
                            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                        }))
                .exceptionally(ex -> {
                    log.error("Error retrieving account by ID {}: {}", id, ex.getMessage());
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }

    /**
     * Retrieves an account by its unique account number.
     * HTTP Method: GET
     * Endpoint: /api/accounts/by-account-number/{accountNumber}
     * Response: Account object with HTTP status 200 (OK) if found, 404 (Not Found) otherwise.
     *
     * @param accountNumber The unique business account number to retrieve.
     * @return A CompletableFuture containing ResponseEntity with the Account and HTTP status.
     */
    @GetMapping("/by-account-number/{accountNumber}")
    public CompletableFuture<ResponseEntity<Account>> getAccountByAccountNumber(@PathVariable String accountNumber) {
        log.info("Received request to get account by account number: {}", accountNumber);
        return accountService.getAccountByAccountNumber(accountNumber)
                .thenApply(optionalAccount -> optionalAccount
                        .map(account -> {
                            log.info("Account found for account number: {}", accountNumber);
                            return new ResponseEntity<>(account, HttpStatus.OK);
                        })
                        .orElseGet(() -> {
                            log.warn("Account not found for account number: {}", accountNumber);
                            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                        }))
                .exceptionally(ex -> {
                    log.error("Error retrieving account by account number {}: {}", accountNumber, ex.getMessage());
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }

    /**
     * Deposits a specified amount into an account.
     * HTTP Method: POST
     * Endpoint: /api/accounts/{accountNumber}/deposit
     * Request Body: DepositRequest object (JSON)
     * Response: Updated Account object with HTTP status 200 (OK) if successful,
     * 400 (Bad Request) for invalid amount, 404 (Not Found) if account doesn't exist,
     * 500 (Internal Server Error) for other issues.
     *
     * @param accountNumber The account number to deposit into.
     * @param request The DepositRequest containing the amount to deposit.
     * @return A CompletableFuture containing ResponseEntity with the updated Account and HTTP status.
     */
    @PostMapping("/{accountNumber}/deposit")
    public CompletableFuture<ResponseEntity<Account>> deposit(
            @PathVariable String accountNumber,
            @RequestBody DepositRequest request) {
        log.info("Received request to deposit {} into account {}", request.getAmount(), accountNumber);
        return accountService.deposit(accountNumber, request.getAmount())
                .thenApply(optionalAccount -> Optional.of(optionalAccount)
                        .map(account -> {
                            log.info("Deposit successful for account {}. New balance: {}", accountNumber, account.getBalance());
                            return new ResponseEntity<>(account, HttpStatus.OK);
                        })
                        .orElseGet(() -> {
                            // This case should ideally be caught by exception handling from service,
                            // but as a fallback, it signifies account not found if optional is empty.
                            log.warn("Deposit failed: Account {} not found (after service call).", accountNumber);
                            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                        }))
                .exceptionally(ex -> {
                    log.error("Error during deposit into account {}: {}", accountNumber, ex.getMessage());
                    if (ex.getCause() instanceof IllegalArgumentException) {
                        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                    } else if (ex.getCause() instanceof RuntimeException && ex.getCause().getMessage().contains("Account not found")) {
                        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                    }
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }

    /**
     * Withdraws a specified amount from an account.
     * HTTP Method: POST
     * Endpoint: /api/accounts/{accountNumber}/withdraw
     * Request Body: WithdrawRequest object (JSON)
     * Response: Updated Account object with HTTP status 200 (OK) if successful,
     * 400 (Bad Request) for invalid amount or insufficient funds, 404 (Not Found) if account doesn't exist,
     * 500 (Internal Server Error) for other issues.
     *
     * @param accountNumber The account number to withdraw from.
     * @param request The WithdrawRequest containing the amount to withdraw.
     * @return A CompletableFuture containing ResponseEntity with the updated Account and HTTP status.
     */
    @PostMapping("/{accountNumber}/withdraw")
    public CompletableFuture<ResponseEntity<Account>> withdraw(
            @PathVariable String accountNumber,
            @RequestBody WithdrawRequest request) {
        log.info("Received request to withdraw {} from account {}", request.getAmount(), accountNumber);
        return accountService.withdraw(accountNumber, request.getAmount())
                .thenApply(optionalAccount -> Optional.of(optionalAccount)
                        .map(account -> {
                            log.info("Withdrawal successful for account {}. New balance: {}", accountNumber, account.getBalance());
                            return new ResponseEntity<>(account, HttpStatus.OK);
                        })
                        .orElseGet(() -> {
                            // Similar to deposit, this fallback should rarely be hit if exceptions are handled well.
                            log.warn("Withdrawal failed: Account {} not found (after service call).", accountNumber);
                            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                        }))
                .exceptionally(ex -> {
                    log.error("Error during withdrawal from account {}: {}", accountNumber, ex.getMessage());
                    if (ex.getCause() instanceof IllegalArgumentException) {
                        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                    } else if (ex.getCause() instanceof RuntimeException) {
                        if (ex.getCause().getMessage().contains("Insufficient funds")) {
                            return new ResponseEntity<>(HttpStatus.BAD_REQUEST); // More specific error code
                        } else if (ex.getCause().getMessage().contains("Account not found")) {
                            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                        }
                    }
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }

    /**
     * Deletes an account by its account number.
     * HTTP Method: DELETE
     * Endpoint: /api/accounts/{accountNumber}
     * Response: HTTP status 204 (No Content) if deleted, 404 (Not Found) if not found.
     *
     * @param accountNumber The account number of the account to delete.
     * @return A CompletableFuture containing ResponseEntity with no content and HTTP status.
     */
    @DeleteMapping("/{accountNumber}")
    public CompletableFuture<ResponseEntity<Object>> deleteAccount(@PathVariable String accountNumber) {
        log.info("Received request to delete account: {}", accountNumber);
        // The service method returns CompletableFuture<Boolean>
        return accountService.deleteAccount(accountNumber)
                .thenApply(deleted -> { // 'deleted' is the boolean result from the service
                    if (deleted) {
                        log.info("Account {} deleted successfully.", accountNumber);
                        return ResponseEntity.noContent().build();
                    } else {
                        log.warn("Account {} not found for deletion.", accountNumber);
                        return ResponseEntity.notFound().build();
                    }
                })
                .exceptionally(ex -> {
                    log.error("Error deleting account {}: {}", accountNumber, ex.getMessage());
                    // This catches any actual exceptions thrown by the service (e.g., SQL errors)
                    return ResponseEntity.internalServerError().build(); // Explicitly returns ResponseEntity<Void> for 500
                });
    }

    // --- DTOs (Data Transfer Objects) ---

    /**
     * Request DTO for deposit operations.
     */
    public static class DepositRequest {
        private BigDecimal amount;

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    /**
     * Request DTO for withdrawal operations.
     */
    public static class WithdrawRequest {
        private BigDecimal amount;

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    /**
     * Request DTO for transfer operations.
     */
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
