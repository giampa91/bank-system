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

@Repository
public class AccountRepository {

    private static final Logger log = LoggerFactory.getLogger(AccountRepository.class);
    private final HikariDataSource dataSource;

    public AccountRepository(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Account save(Account account) {
        String sql = "INSERT INTO account (account_number, user_id, balance, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
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

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    account.setId(generatedKeys.getLong(1));
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
    }

    public Optional<Account> findById(Long id) {
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
    }

    public Optional<Account> findByAccountNumber(String accountNumber) {
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
    }

    public Optional<Account> updateBalance(String accountNumber, BigDecimal newBalance) {
        String sql = "UPDATE account SET balance = ?, updated_at = ? WHERE account_number = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, newBalance);
            stmt.setTimestamp(2, Timestamp.from(Instant.now()));
            stmt.setString(3, accountNumber);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                log.info("Account {} balance updated to {}", accountNumber, newBalance);
                return findByAccountNumber(accountNumber);
            }
            log.warn("Account {} not found for balance update.", accountNumber);
            return Optional.empty();
        } catch (SQLException e) {
            log.error("Error updating account {} balance to {}: {}", accountNumber, newBalance, e.getMessage());
            throw new RuntimeException("Failed to update account balance", e);
        }
    }

    public boolean deleteByAccountNumber(String accountNumber) {
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
    }

    private Account mapResultSetToAccount(ResultSet rs) throws SQLException {
        Account account = new Account();
        account.setId(rs.getLong("id"));
        account.setAccountNumber(rs.getString("account_number"));
        account.setUserId(rs.getLong("user_id"));
        account.setBalance(rs.getBigDecimal("balance"));

        Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
        if (createdAtTimestamp != null) {
            account.setCreatedAt(createdAtTimestamp.toInstant());
        } else {
            account.setCreatedAt(null);
        }

        Timestamp updatedAtTimestamp = rs.getTimestamp("updated_at");
        if (updatedAtTimestamp != null) {
            account.setUpdatedAt(updatedAtTimestamp.toInstant());
        } else {
            account.setUpdatedAt(null);
        }
        return account;
    }
}
