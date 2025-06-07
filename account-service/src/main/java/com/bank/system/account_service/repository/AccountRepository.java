package com.bank.system.account_service.repository;

import com.bank.system.account_service.domain.Account;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.Optional;
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

    /**
     * Saves a new account record to the database.
     *
     * @param account The Account object to save.
     * @return A CompletableFuture that completes with the saved Account object.
     */
    public CompletableFuture<Account> save(Account account) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO account (account_number, user_id, balance, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = dataSource.getConnection();
                 // Use Statement.RETURN_GENERATED_KEYS to get the auto-generated ID
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                account.setCreatedAt(Instant.now());
                account.setUpdatedAt(Instant.now());

                stmt.setString(1, account.getAccountNumber());
                stmt.setLong(2, account.getUserId());
                stmt.setBigDecimal(3, account.getBalance());
                stmt.setTimestamp(4, Timestamp.from(account.getCreatedAt()));
                stmt.setTimestamp(5, Timestamp.from(account.getUpdatedAt()));

                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("Creating account failed, no rows affected.");
                }

                // Retrieve the auto-generated ID
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        account.setId(generatedKeys.getLong(1)); // Assuming 'id' is a BIGINT
                    } else {
                        throw new SQLException("Creating account failed, no ID obtained.");
                    }
                }

                log.info("Account saved: {}", account.getAccountNumber());
                return account;
            } catch (SQLException e) {
                log.error("Error saving account {}: {}", account.getAccountNumber(), e.getMessage());
                throw new RuntimeException("Failed to save account", e);
            }
        }, virtualThreadExecutor);
    }

    /**
     * Finds an account by its primary key ID.
     *
     * @param id The internal ID of the account.
     * @return A CompletableFuture that completes with an Optional containing the Account if found.
     */
    public CompletableFuture<Optional<Account>> findById(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT id, account_number, user_id, balance, created_at, updated_at FROM account WHERE id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToAccount(rs));
                    }
                    return Optional.empty();
                }
            } catch (SQLException e) {
                log.error("Error finding account by ID {}: {}", id, e.getMessage());
                throw new RuntimeException("Failed to find account by ID", e);
            }
        }, virtualThreadExecutor);
    }

    /**
     * Finds an account by its unique account number.
     *
     * @param accountNumber The business account number.
     * @return A CompletableFuture that completes with an Optional containing the Account if found.
     */
    public CompletableFuture<Optional<Account>> findByAccountNumber(String accountNumber) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT id, account_number, user_id, balance, created_at, updated_at FROM account WHERE account_number = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, accountNumber);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToAccount(rs));
                    }
                    return Optional.empty();
                }
            } catch (SQLException e) {
                log.error("Error finding account by account number {}: {}", accountNumber, e.getMessage());
                throw new RuntimeException("Failed to find account by account number", e);
            }
        }, virtualThreadExecutor);
    }

    /**
     * Updates the balance of an existing account.
     *
     * @param accountNumber The account number of the account to update.
     * @param newBalance    The new balance to set.
     * @return A CompletableFuture that completes with an Optional containing the updated Account object, or empty if not found.
     */
    public CompletableFuture<Optional<Account>> updateBalance(String accountNumber, BigDecimal newBalance) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "UPDATE account SET balance = ?, updated_at = ? WHERE account_number = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setBigDecimal(1, newBalance);
                stmt.setTimestamp(2, Timestamp.from(Instant.now()));
                stmt.setString(3, accountNumber);

                int affectedRows = stmt.executeUpdate();
                if (affectedRows > 0) {
                    log.info("Account {} balance updated to {}", accountNumber, newBalance);
                    return findByAccountNumber(accountNumber).join(); // Fetch and return the updated account
                }
                log.warn("Account {} not found for balance update.", accountNumber);
                return Optional.empty();
            } catch (SQLException e) {
                log.error("Error updating account {} balance to {}: {}", accountNumber, newBalance, e.getMessage());
                throw new RuntimeException("Failed to update account balance", e);
            }
        }, virtualThreadExecutor);
    }

    /**
     * Deletes an account by its account number.
     *
     * @param accountNumber The account number of the account to delete.
     * @return A CompletableFuture that completes with true if the account was deleted, false otherwise.
     */
    public CompletableFuture<Boolean> deleteByAccountNumber(String accountNumber) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM account WHERE account_number = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, accountNumber);
                int affectedRows = stmt.executeUpdate();
                if (affectedRows > 0) {
                    log.info("Account {} deleted successfully.", accountNumber);
                    return true;
                } else {
                    log.warn("Account {} not found for deletion.", accountNumber);
                    return false;
                }
            } catch (SQLException e) {
                log.error("Error deleting account {}: {}", accountNumber, e.getMessage());
                throw new RuntimeException("Failed to delete account", e);
            }
        }, virtualThreadExecutor);
    }

    private Account mapResultSetToAccount(ResultSet rs) throws SQLException {
        Account account = new Account();
        account.setId(rs.getLong("id"));
        account.setAccountNumber(rs.getString("account_number"));
        account.setUserId(rs.getLong("user_id"));
        account.setBalance(rs.getBigDecimal("balance"));
//        account.setCreatedAt(rs.getTimestamp("created_at").toInstant());
//        account.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return account;
    }
}