package com.bank.system.account_service.repository;
import com.bank.system.account_service.domain.Account;
import com.bank.system.account_service.service.AccountService;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Repository
public class AccountRepository {

    private static final Logger log = LoggerFactory.getLogger(AccountRepository.class);
    private final HikariDataSource dataSource;
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public AccountRepository(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public CompletableFuture<Account> save(Account account) {
        return CompletableFuture.supplyAsync(() -> account, virtualThreadExecutor);
    }
}
