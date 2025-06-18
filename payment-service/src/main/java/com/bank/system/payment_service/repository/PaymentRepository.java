package com.bank.system.payment_service.repository;

import com.bank.system.payment_service.domain.Payment;
import com.bank.system.payment_service.domain.PaymentStatus;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PaymentRepository {

    private static final Logger log = LoggerFactory.getLogger(PaymentRepository.class);
    private final HikariDataSource dataSource;

    public PaymentRepository(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Payment save(Payment payment) {
        String sql = "INSERT INTO payment (id, sender_account_id, receiver_account_id, amount, currency, status, idempotency_key, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            payment.setId(UUID.randomUUID());
            payment.setCreatedAt(Instant.now());
            payment.setUpdatedAt(Instant.now());

            stmt.setObject(1, payment.getId());
            stmt.setString(2, payment.getSenderAccountId());
            stmt.setString(3, payment.getReceiverAccountId());
            stmt.setBigDecimal(4, payment.getAmount());
            stmt.setString(5, payment.getCurrency());
            stmt.setString(6, payment.getStatus().name());
            stmt.setString(7, payment.getIdempotencyKey());
            stmt.setTimestamp(8, Timestamp.from(payment.getCreatedAt()));
            stmt.setTimestamp(9, Timestamp.from(payment.getUpdatedAt()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating payment failed, no rows affected.");
            }
            log.info("Payment saved: {}", payment.getId());
            return payment;
        } catch (SQLException e) {
            log.error("Error saving payment {}: {}", payment.getId(), e.getMessage());
            throw new RuntimeException("Failed to save payment", e);
        }
    }

    public Optional<Payment> findById(UUID id) {
        String sql = "SELECT id, sender_account_id, receiver_account_id, amount, currency, status, idempotency_key, created_at, updated_at FROM payment WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPayment(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            log.error("Error finding payment by ID {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to find payment by ID", e);
        }
    }

    public Optional<Payment> findByIdempotencyKeyId(String idempotencyKey) {
        String sql = "SELECT id, sender_account_id, receiver_account_id, amount, currency, status, idempotency_key, created_at, updated_at FROM payment WHERE idempotency_key = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, idempotencyKey);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPayment(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            log.error("Error finding payment idempotency key {}: {}", idempotencyKey, e.getMessage());
            throw new RuntimeException("Failed to find payment by idempotency key", e);
        }
    }

    public Optional<Payment> updateStatus(UUID id, PaymentStatus newStatus) {
        String sql = "UPDATE payment SET status = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newStatus.name());
            stmt.setTimestamp(2, Timestamp.from(Instant.now()));
            stmt.setObject(3, id);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                log.info("Payment {} status updated to {}", id, newStatus);
                return findById(id);
            }
            log.warn("Payment {} not found for status update.", id);
            return Optional.empty();
        } catch (SQLException e) {
            log.error("Error updating payment {} status to {}: {}", id, newStatus, e.getMessage());
            throw new RuntimeException("Failed to update payment status", e);
        }
    }

    private Payment mapResultSetToPayment(ResultSet rs) throws SQLException {
        Payment payment = new Payment();
        payment.setId(rs.getObject("id", UUID.class));
        payment.setSenderAccountId(rs.getString("sender_account_id"));
        payment.setReceiverAccountId(rs.getString("receiver_account_id"));
        payment.setAmount(rs.getBigDecimal("amount"));
        payment.setCurrency(rs.getString("currency"));
        payment.setStatus(PaymentStatus.valueOf(rs.getString("status")));
        payment.setIdempotencyKey(rs.getString("idempotency_key"));
        payment.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        payment.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return payment;
    }
}
